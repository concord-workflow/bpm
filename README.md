[![Maven Central](https://img.shields.io/maven-central/v/io.takari.bpm/parent.svg?maxAge=28800)](https://central.sonatype.com/artifact/io.takari.bpm/parent)

# BPM Engine

A simple implementation of a BPM engine, designed to mimic Activiti's behaviour. Optimized for high performance
(rather than strict following of the BPMN specification).

## Requirements
- Java 25+
- Maven 3.9+ (supports `mvnd`)

## Main features
- a lightweight BPM engine, inspired by Activiti BPM engine;
- pluggable state and event persistence;
- event scheduling (e.g. "timers");
- supports Activiti's XML format (both process and visual elements);
- supports Jakarta Expression Language (EL) in flow expressions, task delegates, etc;
- form service API for creating form-based user tasks;
- JUnit Jupiter support (and easy unit testing in general).

## Supported elements:
- boundary event (errors and timers);
- call activity;
- end event;
- event-based gateway;
- exclusive gateway;
- inclusive gateway;
- intermediate catch event;
- parallel gateway;
- script task (JSR-223, GraalJS);
- sequence flow;
- service task;
- start event;
- subprocess;
- user task.

## Limitations
- tasks with TimerBoundaryEvents executed in a separate thread inside of an unbounded Executor.
- currently there is an unbound storage for element activation records. It is used internally in the engine and can
cause large memory footprint in processes with huge amount of looping (hundreds of thousands loops).

## Prior work
Based on the original implementation at [ibodrov/bpm](https://github.com/ibodrov/bpm).
Original contributors:
- Ivan Bodrov <ibodrov@gmail.com>
- Yuri Brigadirenko <ybrigo@gmail.com>
