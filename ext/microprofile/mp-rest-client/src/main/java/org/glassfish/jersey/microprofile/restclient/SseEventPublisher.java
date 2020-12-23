package org.glassfish.jersey.microprofile.restclient;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.sse.InboundSseEvent;
import org.glassfish.jersey.client.ChunkParser;
import org.glassfish.jersey.client.ChunkedInput;
import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class SseEventPublisher extends ChunkedInput<InboundEvent> implements Publisher<InboundEvent> {

    private final Executor executor;
    private final Type genericType;
    private final int BUFFER_SIZE = 512;

    /**
     * SSE event chunk parser - SSE chunks are delimited with a fixed "\n\n" and
     * "\r\n\r\n" delimiter in the response stream.
     */
    private static final ChunkParser SSE_EVENT_PARSER = ChunkedInput.createMultiParser("\n\n", "\r\n\r\n");

    private static final Logger LOG = Logger.getLogger(SseEventPublisher.class.getName());

    private static final Runnable CLEARED = () -> {
        // sentinel indicating we are done.
    };

    @Override
    public void subscribe(Subscriber subscriber) {
        SseSubscription<InboundEvent> subscription = new SseSubscription<>(subscriber, BUFFER_SIZE);
        subscriber.onSubscribe(subscription);
        Runnable readEventTask = () -> {
            Type typeArgument;
            InboundEvent event;
            if (genericType instanceof ParameterizedType) {
                typeArgument = ((ParameterizedType) genericType).getActualTypeArguments()[0];
                if (typeArgument.equals(InboundSseEvent.class)) {
                    try {
                        while ((event = SseEventPublisher.this.read()) != null) {
                            subscription.emit(event);
                        }
                    } catch (Exception e) {
                        subscription.onError(e);
                        return;
                    }
                    subscription.onCompletion();
                } else {
                    try {
                        while ((event = SseEventPublisher.this.read()) != null) {
                            subscription.emit(event.readData((Class<InboundEvent>) typeArgument));
                        }
                    } catch (Exception e) {
                        subscription.onError(e);
                        return;
                    }
                }
                subscription.onCompletion();
            }
        };
        try {
            executor.execute(readEventTask);
        } catch (RejectedExecutionException e) {
            LOG.log(Level.WARNING, "Executor {0} rejected emit event task", executor);
            new Thread(readEventTask, "SseClientPublisherNewThread").start();
        }
    }

    /**
     * Package-private constructor used by the
     * {@link org.glassfish.jersey.client.ChunkedInputReader}.
     *
     * @param inputStream response input stream.
     * @param annotations annotations associated with response entity.
     * @param mediaType response entity media type.
     * @param headers response headers.
     * @param messageBodyWorkers message body workers.
     * @param propertiesDelegate properties delegate for this request/response.
     */
    SseEventPublisher(InputStream inputStream,
            Type genericType,
            Annotation[] annotations,
            MediaType mediaType,
            MultivaluedMap<String, String> headers,
            MessageBodyWorkers messageBodyWorkers,
            PropertiesDelegate propertiesDelegate,
            ExecutorService executor) {
        super(InboundEvent.class, inputStream, annotations, mediaType, headers, messageBodyWorkers, propertiesDelegate);

        super.setParser(SSE_EVENT_PARSER);
        this.executor = executor;
        this.genericType = genericType;
    }

    /**
     * Processor receiving SSE items from the source and dealing with downstream
     * requests. The items are buffers, and older events are dropped if the
     * buffer is full.
     *
     * @param <T> the type of event
     */
    private static class SseSubscription<T> implements Subscription {

        private final AtomicLong requested = new AtomicLong();
        private final Subscriber<T> subscriber;

        private final Queue<T> queue;
        private final int bufferSize;
        private Throwable failure;
        private volatile boolean done;
        private final AtomicInteger wip = new AtomicInteger();
        private final AtomicReference<Runnable> onTermination;

        SseSubscription(Subscriber<T> subscriber, int bufferSize) {
            this.subscriber = subscriber;
            this.bufferSize = bufferSize;
            this.queue = new ArrayBlockingQueue<>(bufferSize);
            this.onTermination = new AtomicReference<>();
        }

        public void emit(T t) {
            if (done || isCancelled()) {
                return;
            }

            if (t == null) {
                throw new NullPointerException("The received item is `null`");
            }

            if (queue.size() == bufferSize) {
                T item = queue.poll();
                LOG.log(Level.INFO, "Dropping server-sent-event '%s' due to lack of subscriber requests", item);
            }
            queue.offer(t);

            drain();
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                addSubscription(requested, n);
                drain();
            } else {
                cancel();
                subscriber.onError(new IllegalArgumentException(
                        "Request must be positive, but was " + n));
            }
        }

        @Override
        public final void cancel() {
            cleanup();
        }

        private boolean isCancelled() {
            return onTermination.get() == CLEARED;
        }

        private void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            final Queue<T> q = queue;

            do {
                long requests = requested.get();
                long emitted = 0L;

                while (emitted != requests) {
                    // Be sure to clear the queue after cancellation or termination.
                    if (isCancelled()) {
                        q.clear();
                        return;
                    }

                    boolean d = done;
                    T event = q.poll();
                    boolean empty = event == null;

                    // No event and done - completing.
                    if (d && empty) {
                        if (failure != null) {
                            sendErrorToSubscriber(failure);
                        } else {
                            sendCompletionToSubscriber();
                        }
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    // Passing the item to subscriber, and incrementing the emitted counter.
                    try {
                        subscriber.onNext(event);
                    } catch (Throwable x) {
                        cancel();
                    }
                    emitted++;
                }

                // We have emitted all the items we could possibly do without violating the protocol.
                if (emitted == requests) {
                    // Be sure to clear the queue after cancellation or termination.
                    if (isCancelled()) {
                        q.clear();
                        return;
                    }

                    // Re-check for completion.
                    boolean d = done;
                    boolean empty = q.isEmpty();
                    if (d && empty) {
                        if (failure != null) {
                            sendErrorToSubscriber(failure);
                        } else {
                            sendCompletionToSubscriber();
                        }
                        return;
                    }
                }

                // Update `requested`
                if (emitted != 0) {
                    producedSubscription(requested, emitted);
                }

                missed = wip.addAndGet(-missed);
            } while (missed != 0);
        }

        protected void onCompletion() {
            done = true;
            drain();
        }

        protected void onError(Throwable e) {
            if (done || isCancelled()) {
                return;
            }

            if (e == null) {
                throw new NullPointerException("Reactive Streams Rule 2.13 violated: The received error is `null`");
            }

            this.failure = e;
            done = true;

            drain();
        }

        private void cleanup() {
            Runnable action = onTermination.getAndSet(CLEARED);
            if (action != null && action != CLEARED) {
                action.run();
            }
        }

        private void sendCompletionToSubscriber() {
            if (isCancelled()) {
                return;
            }
            try {
                subscriber.onComplete();
            } finally {
                cleanup();
            }
        }

        private void sendErrorToSubscriber(Throwable e) {
            if (e == null) {
                e = new NullPointerException("The received error is `null`");
            }
            if (isCancelled()) {
                return;
            }
            try {
                subscriber.onError(e);
            } finally {
                cleanup();
            }
        }

        /**
         * Atomically adds the positive value n to the requested value in the
         * AtomicLong and caps the result at Long.MAX_VALUE and returns the
         * previous value.
         *
         * @param requested the AtomicLong holding the current requested value
         * @param requests the value to add, must be positive (not verified)
         * @return the original value before the add
         */
        public static long addSubscription(AtomicLong requested, long requests) {
            for (;;) {
                long r = requested.get();
                if (r == Long.MAX_VALUE) {
                    return Long.MAX_VALUE;
                }
                long u = r + requests;
                if (u < 0) {
                    u = Long.MAX_VALUE;
                }
                if (requested.compareAndSet(r, u)) {
                    return r;
                }
            }
        }

        /**
         * Concurrent subtraction bound to 0, mostly used to decrement a request
         * tracker by the amount produced by the operator.
         *
         * @param requested the atomic long keeping track of requests
         * @param amount delta to subtract
         * @return value after subtraction or zero
         */
        public static long producedSubscription(AtomicLong requested, long amount) {
            long r;
            long u;
            do {
                r = requested.get();
                if (r == 0 || r == Long.MAX_VALUE) {
                    return r;
                }
                u = r - amount;
                if (u < 0) {
                    u = 0;
                }
            } while (!requested.compareAndSet(r, u));

            return u;
        }

    }

}
