# Notes on the code base


## Skipping tests involving the frontend

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
