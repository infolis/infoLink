infoLink
========

[![Build Status](https://travis-ci.org/infolis/infoLink.svg?branch=master)](https://travis-ci.org/infolis/infoLink)

Overview
--------

infoLink contains algorithm and a thin HTTP API layer that implement the
[InFoLiS](http://infolis.github.io) backend for finding hidden references to
datasets in publications.

Development
-----------

infoLink uses [gradle](http://gradle.org) as its build tool.

### Setup Eclipse for infoLink

Change into your Eclipse workspace directory

```
cd ~/workspace
```

Clone the repository

```
git clone https://github.com/infolis/infoLink
```

Change to the directory and run the `eclipse` gradle task

```
cd infoLink
gradle eclipse
```

This will generate the `.project` and `.metadata` files Eclipse expects.

Then import the project into your Eclipse workspace:

* File -> Import -> Existing Project
* Select the folder `infoLink`
* Choose **not** to copy it to the workspace

Make sure that `src/main/java`, `src/main/resources`, `src/test/java`,
`src/test/resources` are in your Build Path.

We recommend you install the [MoreUnit](http://...) Eclipse plugin to easily
switch between classes and their unit tests.

### Testing

`gradle test` will run all the unit tests.

`gradle test -DinfolisRemoteTest=true` will run integration tests with the remote server in addition to the unit tests.


### Building

To generate a WAR archive ready for deployment in a Servlet container like Tomcat or Jetty:

```
gradle war
```

To setup an ad-hoc Jetty server running the application on port 8080:

`gradle appStart` starts the development backend HTTP API.

If in doubt, try different servlet containers for [gretty](…)

### Development Notes

See [docs/CODE.md](./docs/CODE.md) for more details.
