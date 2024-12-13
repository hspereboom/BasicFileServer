### About
`Arachnid` is a simple file server meant for testing purposes; either publishes
a local directory or redirects to a remote fileserver.

### Integration
The main package (namespace) is `BFS`; please rebrand at will.

### Configuration
The config file `.arachnid` should be available as a resource to the system CL,
i.e. placed in the working directory or embedded in a jar (at the root) that's
on the classpath.

e.g. for Maven projects, one might typically use `src/test/resources`.

### Command Line
Java-NIO-based:  
 `java -cp target/test-classes <package>.BasicFileServerNIO`

Java-IO-based:  
 `java -cp target/test-classes <package>.BasicFileServer`

### Licensing
All code is distributed under the MIT license https://opensource.org/license/mit.  
For easy comparison with other licenses, see https://choosealicense.com/licenses.
