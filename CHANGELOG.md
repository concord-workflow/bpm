# Change Log

## [0.21.1] - 2017-04-02

### Changed

- fix form defaults: copy existing values before calculating default field values.



## [0.21.0] - 2017-03-30

### Added

- `FormField#allowedValue` allows to specify an allowed value for a field. The value
can be of any (serializable) type, process using `ExpressionManager#interpolate` and
can contain expressions.

### Breaking

- `FormField#valueExpr` replaced with `FormField#defaultValue`. This value will be
processed using `ExpressionManager#interpolate` and can contain expressions.



## [0.20.1] - 2017-03-27

### Changed

- new parent POM version;



## [0.20.0] - 2017-03-26

### Added

- `ProcessDefinitionBuilder#script` method for adding `ScriptTask` elements;
- new method in `ProcessDefinitionBuilder` to add user tasks.
- support for form-based user tasks, a new `UserTask.Extension` and forms API.



## [0.14.2] - 2017-03-14

### Added

- support for values interpolation in in/out variable mappings. Any string in source values
(including in deeply-nested collections) can be treated as an expression and eval'ed with
the current process context. This feature available only on the model API level for now:
see `VariableMapping#interpolateValue`.



## [0.14.1] - 2017-03-12

### Added

- evaluation results of `ExpressionType.SIMPLE` tasks now are stored with the
`ServiceTask.EXPRESSION_RESULT_VAR` key in the context. This can be turned off by
setting `Configuration#setStoreExpressionEvalResultsInContext(false)`;
- new methods in `ProcessDefinitionBuilder` to add tasks with IN/OUT variables mapping;
- small improvements for `ProcessDefinitionHelper#dump`.

### Changed

- `DefaultExpressionManager` now logs evaluation errors using `WARN` level.



## [0.14.0] - 2017-03-11

### Added

- new element: `ScriptTask`. Allows execution of JSR-223 compatible scripts;
- new extension point: `ResourceResolver`. Used for resolving external resources (e.g. scripts).



## [0.13.0] - 2017-03-10

### Added

- new element: `UserTask`;
- new extension point: `UserTaskHandler` interface to handle `UserTask` elements. Default implementation
will simply skip those elements.



## [0.12.0] - 2017-02-28

### Added

- basic support for in/out variables in `ServiceTask` elements.
- new `ProcessDefinitionBuilder`.



## [0.11.0] - 2016-12-12

### Added

- initial implementation of the `EventService` API.

### Changed

- serialization is working again (including the serialization benchmark).



## [0.10.3] - 2016-12-08

### Changed

- fixed the bug, preventing processes with multiple calls to a same activity from working with parallel or
inclusive gateways.
- memory footprint optimizations.



## [0.10.2] - 2016-11-15

### Added

- the new methods `ExecutionInterceptor#onScopeCreated` and `#onScopeDestroyed` allow to monitor logical scopes
creation and removal.

### Deprecated

- `ExecutionInterceptor#onError(String, Throwable)` superseded by `ExecutionInterceptor(InterceptorErrorEvent)`.



## [0.10.1] - 2016-11-10 

### Added

- the `ProcessDefinitionHelper#dump` utility method to print a process structure as a String.



## [0.10.0] - 2016-11-05

### Added

- the new `ExecutionInterceptor#onUnhandledError` method can be used to track unhandled errors in suspended processes.
E.g., in situations when one of `Engine#resume` branches fails, but the process is not finished due to other events
waiting.

### Changed

- do not throw unhandled BPMN errors if a process is not finished.
- the `ExecutionContext#PROCESS_BUSINESS_KEY` variable is now usable again.
- fixed `ExecutionInterceptor#onElement` not invoking for boundary event elements.

### Breaking

- `Configuration#throwExceptionOnErrorEnd` is removed. See `Configuration#unhandledBpmnErrorStrategy = PROPAGATE`.



## [0.9.5] - 2016-11-02

### Changed

- fixed the scoping issue with subprocesses and error propagation.
- minor logging improvements.



## [0.9.4] - 2016-11-01

### Added

- avoid reloading a process definition on each `CallActivity` invocation. Enabled by the 
`Configuration#avoidDefinitionReloadingOnCall` flag.
- the new method `AbstractEngine#run(ProcessInstance)` to run a process using a state's snapshot.
- `Configuration#unhandledBpmnErrorStrategy` allows to specify the strategy for unhandled BPMN errors:
    - `EXCEPTION` - immediately throw an exception;
    - `PROPAGATE` - propagate an error to an higher level;
    - `IGNORE` - ignore an error and continue an execution.
- `BpmnError` now contains a process definition ID and an element ID of an error's source.

# Changed

- a few uninformative log records moved to the `DEBUG` level.



## [0.9.3] - 2016-10-14

### Added

- `EngineBuilder#wrapDefinitionProviderWith` now can be used to decorate definition providers with some additional
functionality like caching (see `CachingIndexedProcessDefinitionProvider`).
- `SourceAwareProcessDefinition` allows to store "source maps" - an additional metadata that links a process
definition's source and resulting data structures.



## [0.9.2] - 2016-10-07

### Added

- more tests.
- automatically cleanup free scopes. Reduces the memory footprint and strain on serialization.
- `IntermediateCatchEvent#messageRefExpression` can now be used to generate event names. The result of evaluation must
be a `String`.



## [0.9.1] - 2016-10-07

### Added

- `EndEvent` now can collect the cause of an error with an expression. Such expressions must return an instance of
`Throwable`.

### Breaking

- `ExecutionContext#ERROR_CODE_KEY` removed in favor of `#LAST_ERROR_KEY`. The new key is used to retrieve latest
handled `BpmnError`.



## [0.9.0] - 2016-10-07

### Added

- `ExecutionContext` now can be accessed from the `ScriptingExpressionManager` (just like in the
`DefaultExpressionManager`).
- `SubProcess` now supports out variables.

### Breaking

- `Event#groupId` renamed to `Event#scopeId`. This may break deserialization of pre-existing data.

### Changed

- Event scoping is completely rewritten in order to support more complex use cases.



## [0.8.9] - 2016-10-02

### Added

- a convenience constructor: `Edge#(id, elementId, label, waypoints)`.
- an optional ability to discard all changes to variables made in a subprocess with the
`Subprocess#isUseSeparateContext` flag.
- if the `Configuration#throwExceptionOnUnhandledBpmnError` flag is set then any unhandled (e.g. without a boundary
error event) `BpmnError` will throw an `ExecutionException`.



## [0.8.8] - 2016-09-23

### Added

- `VariableMapping` now accepts a `sourceValue` in case if one needs to pass a raw value into a subprocess.



## [0.8.7] - 2016-09-19

### Added

- additional behaviour tweaks now can be configured via `EngineBuilder#withConfiguration`.
- introduced `Configuration#throwExceptionOnErrorEnd`. When enabled, the engine will throw an `ExecutionException` if
process ends with an unhandled error end event.
- now it is possible to override a thread pool used by boundary timer events with the
`EngineBuilder#withThreadPool` method.



## [0.8.6] - 2016-09-11

### Added
- a new constructor in the `DefaultExpressionManager` class allows customization of EL resolvers.
- process definition's attributes now accessible as `ExecutionContext` variables.



## [0.8.5] - 2016-09-02

### Added
- more fine-grained control over process deployment in `EngineRule`.
- minor logging improvements.
- `ProcessDefinition` can now contain additional attributes (e.g. parser's metadata).

### Changed
- clarify the javadoc on the `ProcessDefinitionProvider#getById` method: it returns `null` if process is not found instead
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
