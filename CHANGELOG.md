# Change Log

## [Unreleased]

### Added
- more fine-grained control over process deployment in `EngineRule`.
- minor logging improvements.

### Changed
- clarify the javadoc on the `ProcessDefinitionProvider#getById` method: it returns `null` if process not found instead
of throwing an exception.



## [0.8.4] - 2016-08-29

### Added
- new interceptor's method `ExecutionInterceptor#onFailure(businessKey, errorRef)`, called for unhandled BPM
errors.

### Breaking
- `io.takari.bpm.api.interceptors.ElementEvent` renamed to `InterceptorElementEvent`.

### Changed
- disabled caching for indexed instances of `ProcessDefinition`. It will stay disabled until the API provides a way
to notify about process changes in an underlying definition store.



## [0.8.3] - 2016-08-10

### Added

- fallback to the activiti's XML namespace when parsing a `ServiceTask` declaration. This change is to ensure
compatibility with the latest version of Activiti's BPMN editor.

### Breaking

- `bpmnjs-compat` module moved to [Orchestra](https://github.com/takari/orchestra) as the default BPMN parser.

### Changed

- fix for boundary error events handling: when no suitable error references are found, a default one is used.



## [0.8.2] - 2016-08-05

### Added

- `BpmnError#getCause` and the corresponding constructor are added.



## [0.8.1] - 2016-07-28

### Added

- initial support for JSR 223 (scripting for Java) in expressions.
- new method `ExecutionContext#eval` allows evaluation of expressions in `JavaDelegate` tasks.

### Changes

- improved performance of boundary events lookup and handling.
About 5% improvement for the most of scenarios.



## [0.8.0] - 2016-07-23

### Changes

- complete rewrite of internal state management. API and semantics weren't changed.
- fixed more bugs related to deep nested process handling.



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

- `io.takari.bpm.leveldb.LevelDbPersistenceManager` -- a serializer removed from its constructor. It wasn't used in any way, just a relic of the past.



## [0.7.1] - 2016-07-07

### Changed

- a bug was preventing standalone intermediate catch events from working.
- fix a potential classloader problem while initializing JUEL's ExpressionFactory.

## [0.7.0] - 2016-07-07
First public release.
