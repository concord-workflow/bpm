# Detecting actual CPU count: 8 detected
# JMH 1.12 (released 255 days ago, please consider updating!)
# VM version: JDK 1.8.0_111, VM 25.111-b14
# VM invoker: /home/ibodrov/opt/jdk/jdk1.8.0_111/jre/bin/java
# VM options: -Djava.security.egd=file:/dev/./urandom
# Warmup: 5 iterations, 1 s each
# Measurement: 5 iterations, 1 s each
# Timeout: 10 min per iteration
# Threads: 8 threads, will synchronize iterations
# Benchmark mode: Throughput, ops/time
# Benchmark: io.takari.bpm.benchmark.Linear10DelegateBenchmark.test

# Run progress: 0.00% complete, ETA 00:00:30
# Fork: 1 of 1
# Warmup Iteration   1: 17998.209 ops/s
# Warmup Iteration   2: 55733.317 ops/s
# Warmup Iteration   3: 84072.872 ops/s
# Warmup Iteration   4: 86838.957 ops/s
# Warmup Iteration   5: 86690.733 ops/s
Iteration   1: 87245.713 ops/s
Iteration   2: 86333.031 ops/s
Iteration   3: 86308.487 ops/s
Iteration   4: 86100.848 ops/s
Iteration   5: 86031.075 ops/s


Result "test":
  86403.831 ±(99.9%) 1880.139 ops/s [Average]
  (min, avg, max) = (86031.075, 86403.831, 87245.713), stdev = 488.266
  CI (99.9%): [84523.692, 88283.969] (assumes normal distribution)


# JMH 1.12 (released 255 days ago, please consider updating!)
# VM version: JDK 1.8.0_111, VM 25.111-b14
# VM invoker: /home/ibodrov/opt/jdk/jdk1.8.0_111/jre/bin/java
# VM options: -Djava.security.egd=file:/dev/./urandom
# Warmup: 5 iterations, 1 s each
# Measurement: 5 iterations, 1 s each
# Timeout: 10 min per iteration
# Threads: 8 threads, will synchronize iterations
# Benchmark mode: Throughput, ops/time
# Benchmark: io.takari.bpm.benchmark.Linear10JuelInMemBenchmark.test

# Run progress: 33.33% complete, ETA 00:00:21
# Fork: 1 of 1
# Warmup Iteration   1: 21793.799 ops/s
# Warmup Iteration   2: 62090.981 ops/s
# Warmup Iteration   3: 81776.682 ops/s
# Warmup Iteration   4: 84493.917 ops/s
# Warmup Iteration   5: 85237.942 ops/s
Iteration   1: 83520.386 ops/s
Iteration   2: 84334.911 ops/s
Iteration   3: 85166.964 ops/s
Iteration   4: 84146.320 ops/s
Iteration   5: 84205.603 ops/s


Result "test":
  84274.837 ±(99.9%) 2269.925 ops/s [Average]
  (min, avg, max) = (83520.386, 84274.837, 85166.964), stdev = 589.492
  CI (99.9%): [82004.911, 86544.762] (assumes normal distribution)


# JMH 1.12 (released 255 days ago, please consider updating!)
# VM version: JDK 1.8.0_111, VM 25.111-b14
# VM invoker: /home/ibodrov/opt/jdk/jdk1.8.0_111/jre/bin/java
# VM options: -Djava.security.egd=file:/dev/./urandom
# Warmup: 5 iterations, 1 s each
# Measurement: 5 iterations, 1 s each
# Timeout: 10 min per iteration
# Threads: 8 threads, will synchronize iterations
# Benchmark mode: Throughput, ops/time
# Benchmark: io.takari.bpm.benchmark.PersistenceBenchmark.test

# Run progress: 66.67% complete, ETA 00:00:10
# Fork: 1 of 1
# Warmup Iteration   1: 368.989 ops/s
# Warmup Iteration   2: 680.332 ops/s
# Warmup Iteration   3: 684.527 ops/s
# Warmup Iteration   4: 674.867 ops/s
# Warmup Iteration   5: 681.819 ops/s
Iteration   1: 400.192 ops/s
Iteration   2: 234.774 ops/s
Iteration   3: 684.591 ops/s
Iteration   4: 673.429 ops/s
Iteration   5: 684.370 ops/s


Result "test":
  535.471 ±(99.9%) 798.855 ops/s [Average]
  (min, avg, max) = (234.774, 535.471, 684.591), stdev = 207.460
  CI (99.9%): [≈ 0, 1334.326] (assumes normal distribution)


# Run complete. Total time: 00:00:34

Benchmark                         Mode  Cnt      Score      Error  Units
Linear10DelegateBenchmark.test   thrpt    5  86403.831 ± 1880.139  ops/s
Linear10JuelInMemBenchmark.test  thrpt    5  84274.837 ± 2269.925  ops/s
PersistenceBenchmark.test        thrpt    5    535.471 ±  798.855  ops/s
