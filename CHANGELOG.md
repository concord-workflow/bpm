# Change Log

## [0.7.3] - 2016-07-19
### Added
- `CallActivity#copyAllVariables` flag now can be used to copy all variables from the parent process to the called.

## [0.7.2] - 2016-07-18 
### Changed
- a bug was preventing deeply-nested processes from handling events correctly.
- minor code and dependencies cleanup.

### Added
- `bpmnjs-compat` module: a [bpmn.io](http://bpmn.io) compactible xml format parser (only a partial support for the current set of elements).
- `InclusiveGateway` now supports expressions for outgoing flows. Some of outgoing `SequenceFlow` can be "inactive" (have their expressions evaluated to `false`).

### Breaking
- `io.takari.bpm.leveldb.LevelDbPersistenceManager` -- a serialized removed from its constructor. It wasn't used in any way, just a relic of the past.

## [0.7.1] - 2016-07-07
### Changed
- a bug was preventing standalone intermediate catch events from working.
- fix a potential classloader problem while initializing JUEL's ExpressionFactory.

## [0.7.0] - 2016-07-07
First public release.
