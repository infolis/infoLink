# infoLink

## Testing

`gradle test` will run all the unit tests.

`gradle test -DinfolisRemoteTest=true` will run integration tests with the remote server in addition to the unit tests.

## Building

`gradle installDist` will build the Java classes, create startup scripts and
assemble the project into a directory `build/install/infolink`

`gradle appStart` starts the development backend HTTP API.
