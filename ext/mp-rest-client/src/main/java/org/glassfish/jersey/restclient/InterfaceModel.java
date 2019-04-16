/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.jersey.restclient;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;

import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.ext.AsyncInvocationInterceptor;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

/**
 * Model of interface and its annotation.
 *
 * @author David Kral
 */
class InterfaceModel {

    private final Class<?> restClientClass;
    private final String[] produces;
    private final String[] consumes;
    private final String path;
    private final ClientHeadersFactory clientHeadersFactory;
    private final CreationalContext<?> creationalContext;

    private final List<ClientHeaderParamModel> clientHeaders;
    private final List<AsyncInvocationInterceptor> asyncInterceptors;
    private final Set<ResponseExceptionMapper> responseExceptionMappers;
    private final Set<ParamConverterProvider> paramConverterProviders;
    private final Set<Annotation> interceptorAnnotations;

    /**
     * Creates new model based on interface class. Interface is parsed according to specific annotations.
     *
     * @param restClientClass interface class
     * @param responseExceptionMappers registered exception mappers
     * @param paramConverterProviders registered parameter providers
     * @param asyncInterceptors async interceptors
     * @return new model instance
     */
    static InterfaceModel from(Class<?> restClientClass,
                               Set<ResponseExceptionMapper> responseExceptionMappers,
                               Set<ParamConverterProvider> paramConverterProviders,
                               List<AsyncInvocationInterceptor> asyncInterceptors) {
        return new Builder(restClientClass,
                           responseExceptionMappers,
                           paramConverterProviders,
                           asyncInterceptors)
                .pathValue(restClientClass.getAnnotation(Path.class))
                .produces(restClientClass.getAnnotation(Produces.class))
                .consumes(restClientClass.getAnnotation(Consumes.class))
                .clientHeaders(restClientClass.getAnnotationsByType(ClientHeaderParam.class))
                .clientHeadersFactory(restClientClass.getAnnotation(RegisterClientHeaders.class))
                .build();
    }

    private InterfaceModel(Builder builder) {
        this.restClientClass = builder.restClientClass;
        this.path = builder.pathValue;
        this.produces = builder.produces;
        this.consumes = builder.consumes;
        this.clientHeaders = builder.clientHeaders;
        this.clientHeadersFactory = builder.clientHeadersFactory;
        this.responseExceptionMappers = builder.responseExceptionMappers;
        this.paramConverterProviders = builder.paramConverterProviders;
        this.interceptorAnnotations = builder.interceptorAnnotations;
        this.creationalContext = builder.creationalContext;
        this.asyncInterceptors = builder.asyncInterceptors;
    }

    /**
     * Returns rest client interface class.
     *
     * @return interface class
     */
    Class<?> getRestClientClass() {
        return restClientClass;
    }

    /**
     * Returns defined produces media types.
     *
     * @return produces
     */
    String[] getProduces() {
        return produces;
    }

    /**
     * Returns defined consumes media types.
     *
     * @return consumes
     */
    String[] getConsumes() {
        return consumes;
    }

    /**
     * Returns path value defined on interface level.
     *
     * @return path value
     */
    String getPath() {
        return path;
    }

    /**
     * Returns registered instance of {@link ClientHeadersFactory}.
     *
     * @return registered factory
     */
    Optional<ClientHeadersFactory> getClientHeadersFactory() {
        return Optional.ofNullable(clientHeadersFactory);
    }

    /**
     * Returns {@link List} of processed annotation {@link ClientHeaderParam} to {@link ClientHeaderParamModel}
     *
     * @return registered factories
     */
    List<ClientHeaderParamModel> getClientHeaders() {
        return clientHeaders;
    }

    /**
     * Returns {@link List} of registered {@link AsyncInvocationInterceptor}
     *
     * @return registered async interceptors
     */
    List<AsyncInvocationInterceptor> getAsyncInterceptors() {
        return asyncInterceptors;
    }

    /**
     * Returns {@link Set} of registered {@link ResponseExceptionMapper}
     *
     * @return registered exception mappers
     */
    Set<ResponseExceptionMapper> getResponseExceptionMappers() {
        return responseExceptionMappers;
    }

    /**
     * Returns {@link Set} of registered {@link ParamConverterProvider}
     *
     * @return registered param converter providers
     */
    Set<ParamConverterProvider> getParamConverterProviders() {
        return paramConverterProviders;
    }

    /**
     * Returns {@link Set} of interceptor annotations
     *
     * @return interceptor annotations
     */
    Set<Annotation> getInterceptorAnnotations() {
        return interceptorAnnotations;
    }

    /**
     * Context bound to this model.
     *
     * @return context
     */
    CreationalContext<?> getCreationalContext() {
        return creationalContext;
    }

    /**
     * Resolves value of the method argument.
     *
     * @param arg actual argument value
     * @param type type of the argument
     * @param annotations annotations bound to argument
     * @return converted value of argument
     */
    Object resolveParamValue(Object arg, Type type, Annotation[] annotations) {
        for (ParamConverterProvider paramConverterProvider : paramConverterProviders) {
            ParamConverter<Object> converter = paramConverterProvider
                    .getConverter((Class<Object>) type, null, annotations);
            if (converter != null) {
                return converter.toString(arg);
            }
        }
        return arg;
    }

    private static class Builder {

        private final Class<?> restClientClass;

        private String pathValue;
        private String[] produces;
        private String[] consumes;
        private ClientHeadersFactory clientHeadersFactory;
        private CreationalContext<?> creationalContext;
        private List<ClientHeaderParamModel> clientHeaders;
        private List<AsyncInvocationInterceptor> asyncInterceptors;
        private Set<ResponseExceptionMapper> responseExceptionMappers;
        private Set<ParamConverterProvider> paramConverterProviders;
        private Set<Annotation> interceptorAnnotations;

        private Builder(Class<?> restClientClass,
                        Set<ResponseExceptionMapper> responseExceptionMappers,
                        Set<ParamConverterProvider> paramConverterProviders,
                        List<AsyncInvocationInterceptor> asyncInterceptors) {
            this.restClientClass = restClientClass;
            this.responseExceptionMappers = responseExceptionMappers;
            this.paramConverterProviders = paramConverterProviders;
            this.asyncInterceptors = asyncInterceptors;
            filterAllInterceptorAnnotations();
        }

        private void filterAllInterceptorAnnotations() {
            creationalContext = null;
            interceptorAnnotations = new HashSet<>();
            try {
                if (CDI.current() != null) {
                    BeanManager beanManager = CDI.current().getBeanManager();
                    creationalContext = beanManager.createCreationalContext(null);
                    for (Annotation annotation : restClientClass.getAnnotations()) {
                        if (beanManager.isInterceptorBinding(annotation.annotationType())) {
                            interceptorAnnotations.add(annotation);
                        }
                    }
                }
            } catch (IllegalStateException ignored) {
                //CDI not present. Ignore.
            }
        }

        /**
         * Path value from {@link Path} annotation. If annotation is null, empty String is set as path.
         *
         * @param path {@link Path} annotation
         * @return updated Builder instance
         */
        Builder pathValue(Path path) {
            this.pathValue = path != null ? path.value() : "";
            //if only / is added to path like this "localhost:80/test" it makes invalid path "localhost:80/test/"
            this.pathValue = pathValue.equals("/") ? "" : pathValue;
            return this;
        }

        /**
         * Extracts MediaTypes from {@link Produces} annotation.
         * If annotation is null, new String array with {@link MediaType#WILDCARD} is set.
         *
         * @param produces {@link Produces} annotation
         * @return updated Builder instance
         */
        Builder produces(Produces produces) {
            this.produces = produces != null ? produces.value() : new String[] {MediaType.WILDCARD};
            return this;
        }

        /**
         * Extracts MediaTypes from {@link Consumes} annotation.
         * If annotation is null, new String array with {@link MediaType#WILDCARD} is set.
         *
         * @param consumes {@link Consumes} annotation
         * @return updated Builder instance
         */
        Builder consumes(Consumes consumes) {
            this.consumes = consumes != null ? consumes.value() : new String[] {MediaType.WILDCARD};
            return this;
        }

        /**
         * Process data from {@link ClientHeaderParam} annotation to extract methods and values.
         *
         * @param clientHeaderParams {@link ClientHeaderParam} annotations
         * @return updated Builder instance
         */
        Builder clientHeaders(ClientHeaderParam[] clientHeaderParams) {
            clientHeaders = Arrays.stream(clientHeaderParams)
                    .map(clientHeaderParam -> new ClientHeaderParamModel(restClientClass, clientHeaderParam))
                    .collect(Collectors.toList());
            return this;
        }

        Builder clientHeadersFactory(RegisterClientHeaders registerClientHeaders) {
            clientHeadersFactory = registerClientHeaders != null
                    ? ReflectionUtil.createInstance(registerClientHeaders.value())
                    : null;
            return this;
        }

        /**
         * Creates new InterfaceModel instance.
         *
         * @return new instance
         */
        InterfaceModel build() {
            validateHeaderDuplicityNames();
            return new InterfaceModel(this);
        }

        private void validateHeaderDuplicityNames() {
            ArrayList<String> names = new ArrayList<>();
            for (ClientHeaderParamModel clientHeaderParamModel : clientHeaders) {
                String headerName = clientHeaderParamModel.getHeaderName();
                if (names.contains(headerName)) {
                    throw new RestClientDefinitionException("Header name cannot be registered more then once on the same target."
                                                                    + "See " + restClientClass.getName());
                }
                names.add(headerName);
            }
        }
    }
}