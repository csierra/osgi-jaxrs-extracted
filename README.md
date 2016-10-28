## JAX-RS Whiteboard

This is an implementation of a JAX-RS Services whiteboard [OSGi RFC-217](https://github.com/osgi/design/tree/master/rfcs/rfc0217).

## Building

Execute the usual maven task `mvn package`.

## Running the Example

This is a two step process.

### Create the executable jar

In the bndrun subdirectory, execute the task `mvn bnd-export:export`

**Note:** There's an occasional NPE occuring during the resolve operation. If you should encounter this, please try again.

### Run the exported jar

Once exported, the bndrun directory should contain a file `org.apache.aries.jax-rs.example.jar`.

Execute the following command

```
java -jar org.apache.aries.jax-rs.example.jar
```