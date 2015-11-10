# Notes on the code base

## Algorithm contract

*MUST* have public constructors (not package-local, i.e. without explicit modifier)

## Skipping tests involving the frontend or GESIS' network

1) Add code to tests to mark them as remote

```java
public class FooTest {
    @Test
    public void myFooTest() throws Exception
    {
        Assume.assumeNotNull(System.getProperty("infolisRemoteTest"));
    }
}
```

This will skip the test unless the `infolisRemoteTest` system property is set to `true`

2) *If* remote tests should be executed

Set the system property `infolisRemoteTest`:

`java .... -DinfolisRemoteTest=true`

In Eclipse: `Run -> Run Configurations -> Arguments -> VM arguments`

Replace `infolisRemoteTest` with `gesisRemoteTest` for tests requiring a connection from within GESIS.

