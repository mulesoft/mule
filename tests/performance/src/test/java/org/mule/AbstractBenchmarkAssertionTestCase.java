/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static java.lang.System.getProperty;
import static java.lang.Boolean.getBoolean;
import static java.util.Collections.EMPTY_MAP;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Base class for creating benchmark assertion tests allowing JMH benchmark results to be asserted using JUnit tests with
 * Hamcrest.
 */
public abstract class AbstractBenchmarkAssertionTestCase extends AbstractMuleTestCase {

  private static final String ENABLE_PERFORMANCE_TESTS_SYSTEM_PROPERTY = "enablePerformanceTests";
  private static final String NORM_ALLOCATION_RESULT_KEY = "·gc.alloc.rate.norm";

  @Override
  public int getTestTimeoutSecs() {
    return 5 * 60;
  }

  /**
   * Run a JMH benchmark and assert that the primary result is less than or equal to an expected value.
   *
   * @param clazz the JMS benchmark class.
   * @param testName the name of the test method.
   * @param expectedResult the expected minimum minimum result value.
   * @param timeUnit the time unit of the expected result value.
   */
  protected void runAndAssertBenchmark(Class clazz, String testName, final double expectedResult, TimeUnit timeUnit) {
    runAndAssertBenchmark(clazz, testName, 1, expectedResult, timeUnit);
  }

  /**
   * Run a JMH benchmark and assert that the primary result is less than or equal to an expected value.
   *
   * @param clazz the JMS benchmark class.
   * @param testName the name of the test method.
   * @param expectedResult the expected minimum minimum result value.
   * @param timeUnit the time unit of the expected result value.
   * @param expectedAllocation the expected maximum allocation in bytes per benchmark iteration.
   */
  protected void runAndAssertBenchmark(Class clazz, String testName, final double expectedResult, TimeUnit timeUnit,
                                       double expectedAllocation) {
    runAndAssertBenchmark(clazz, testName, 1, EMPTY_MAP, expectedResult, timeUnit, expectedAllocation);
  }

  /**
   * Run a JMH benchmark and assert that the primary result is less than or equal to an expected value.
   *
   * @param clazz the JMS benchmark class.
   * @param testName the name of the test method.
   * @param threads the number of threads to run benchmark with.
   * @param expectedResult the expected minimum minimum result value.
   * @param timeUnit the time unit of the expected result value.
   */
  protected void runAndAssertBenchmark(Class clazz, String testName, int threads, final double expectedResult,
                                       TimeUnit timeUnit) {
    runAndAssertBenchmark(clazz, testName, threads, EMPTY_MAP, timeUnit, false,
                          runResult -> assertThat(runResult.getPrimaryResult().getScore(), lessThanOrEqualTo(expectedResult)));
  }

  /**
   * Run a JMH benchmark and assert that the primary result is less than or equal to an expected value.
   *
   * @param clazz the JMS benchmark class.
   * @param testName the name of the test method.
   * @param threads the number of threads to run benchmark with.
   * @param params parameters along with array of parameters values to be applied to the benchmark.
   * @param expectedResult the expected minimum minimum result value.
   * @param timeUnit the time unit of the expected result value.
   * @param expectedAllocation the expected maximum allocation in bytes per benchmark iteration.
   */
  protected void runAndAssertBenchmark(Class clazz, String testName, int threads, Map<String, String[]> params,
                                       final double expectedResult,
                                       TimeUnit timeUnit, final double expectedAllocation) {
    runAndAssertBenchmark(clazz, testName, threads, params, timeUnit, true,
                          runResult -> {
                            assertThat(runResult.getPrimaryResult().getScore(), lessThanOrEqualTo(expectedResult));
                            assertThat(runResult.getSecondaryResults().get(NORM_ALLOCATION_RESULT_KEY).getScore(),
                                       lessThanOrEqualTo(expectedAllocation));
                          });
  }

  /**
   * Run a JMH benchmark and assert that the primary result is less than or equal to an expected value.
   *
   * @param clazz the JMS benchmark class.
   * @param testName the name of the test method.
   * @param threads the number of threads to run benchmark with.
   * @param params parameters along with array of parameters values to be applied to the benchmark.
   * @param timeUnit the time unit of the expected result value.
   * @param assertions assertion consumer.
   */
  protected void runAndAssertBenchmark(Class clazz, String testName, int threads, Map<String, String[]> params, TimeUnit timeUnit,
                                       boolean profileGC, Consumer<RunResult> assertions) {
    try {
      if (getBoolean(getProperty(ENABLE_PERFORMANCE_TESTS_SYSTEM_PROPERTY))) {
        ChainedOptionsBuilder optionsBuilder = createCommonOptionsBuilder(clazz, testName, params, timeUnit, profileGC);
        optionsBuilder = optionsBuilder
            .forks(1)
            .threads(threads)
            .warmupIterations(10)
            .measurementIterations(10);
        assertions.accept(new Runner(optionsBuilder.build()).runSingle());
      } else {
        ChainedOptionsBuilder optionsBuilder = createCommonOptionsBuilder(clazz, testName, params, timeUnit, profileGC);
        optionsBuilder = optionsBuilder
            .forks(0)
            .warmupIterations(0)
            .measurementIterations(1);
        new Runner(optionsBuilder.build()).runSingle();
      }
    } catch (RunnerException e) {
      fail(e.getMessage());
      e.printStackTrace();
    }
  }

  private ChainedOptionsBuilder createCommonOptionsBuilder(Class clazz, String testName, Map<String, String[]> params,
                                                           TimeUnit timeUnit, boolean profileGC) {
    ChainedOptionsBuilder optionsBuilder = new OptionsBuilder();
    optionsBuilder = optionsBuilder
        .include(clazz.getSimpleName() + "." + testName + "$")
        .timeUnit(timeUnit);
    if (profileGC) {
      optionsBuilder = optionsBuilder.addProfiler(GCProfiler.class);
    }
    for (Entry<String, String[]> entries : params.entrySet()) {
      optionsBuilder = optionsBuilder.param(entries.getKey(), entries.getValue());
    }
    return optionsBuilder;
  }

}
