Recommended command for building patch version

````
mvn clean deploy -Dtests.excluded -Dexamples.excluded
````

Don't run any server on port 8080 during build, as one of the tests assumes that
connection to that port fails with Connection refused.

When changing versions, don't forget to additionally change version of `bom/pom.xml`
as `mvn versions:set` does not propagate to it. Or use `-DprocessAllModules`