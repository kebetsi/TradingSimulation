TradingSimulation
=================

TradingSimulation is a modular framework designed for back-testing trading strategies. The components are built on top of akka Actors.

### Documentation

Detailed documentation can be found on the dedicated [wiki](https://github.com/kebetsi/TradingSimulation/wiki).

### Build:

- Install the [SBT](http://www.scala-sbt.org/) build tool.

- `sbt compile` to compile the source files

- `sbt "project ts" run` displays a list of examples that can be run in the TradingSimulater backend

- `sbt "project frontend" run` starts the frontend. The server is listening for HTTP on http://localhost:9000/

### License

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0