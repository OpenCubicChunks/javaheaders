# Java Headers

This plugin implemenents 'API inversion'

The API consumer defines the required API using classes and interfaces with the @JavaHeader annotation.

The API publisher implements the API provided by the consumer

![image](https://user-images.githubusercontent.com/16853282/184933210-84762efa-96a1-4456-9c3c-9210a0fed5f6.png)

## Example case: project `A` depends on project `B`

`B` defines the API it requires using the `io.github.opencubicchunks.javaheaders.api.Header` annotation like so: (`B` may define as many such classes 
or interfaces as it likes)
```java
@Header
public class Foo {
    public int x;
    public int z;
    
    public void Foo(int x, int z) {
        throw new AbstractMethodError();
    }
    
    public native Bar getBar(); // native is used just so the class compiles, abstract would also work in the case of an abstract class
    
    public native static void Baz();
}

@Header
public interface Bar() { 
    void bin();
}
```

Within its `build.gradle` `A` then applies the `javaHeaders` plugin, and defines its parameters.
```groovy
apply plugin: 'io.github.opencubicchunks.javaheaders'

linkHeadersConfig {
    config = file("javaHeaders.json") // the javaHeaders config json
    acceptedJars = ".*B.*" // match any jar containing the name of our dependency
    debug = false // optional parameter, causes the artifact transformer to always run if true. Even if there are no changes to the inputs
}
```

Finally `A` defines a config file that maps each class of the implementation to the respective headers it implements
```json
{
    "implementations": {
        "a.FooImpl": [ "b.Foo" ],
        "a.BarImpl": [ "b.Bar" ]
    }
}
```
