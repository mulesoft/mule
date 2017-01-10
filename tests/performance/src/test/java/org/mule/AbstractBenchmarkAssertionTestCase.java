/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static java.lang.Boolean.TRUE;
import static java.lang.System.getProperty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Base class for creating benchmark assertion tests allowing JMH benchmark results to be asserted using JUnit tests with Hamcrest.
 */
public abstract class AbstractBenchmarkAssertionTestCase extends AbstractMuleTestCase
{

    private static final String SKIP_PERFORMANCE_TESTS_SYSTEM_PROPERTY = "skipPerformanceTests";
    /**
     * Run a JMH benchmark and assert that the primery result is less than or equal to an expected value.
     *
     * @param clazz the JMS benchmark class.
     * @param testName the name of the test method.
     * @param expectedResult the expected minimum minimum result value.
     * @param timeUnit the time unit of the expected result value.
     */
    protected void runAndAssertBenchmark(Class clazz, String testName, final double expectedResult, TimeUnit timeUnit)
    {
        runAndAssertBenchmark(clazz, testName, timeUnit, new AssertionCallback()
        {
            @Override
            public void assertResult(RunResult runResult)
            {
                assertThat(runResult.getPrimaryResult().getScore(), lessThanOrEqualTo(expectedResult));
            }
        });
    }

    /**
     * Run a JMH benchmark and use a {@link AssertionCallback} to assert the results are withing expected bounds.
     *
     * @param clazz the JMS benchmark class.
     * @param testName the name of the test method.
     * @param timeUnit the timeunit to use for running JMH test.
     * @param assertions an  assertion callback for asserting test results.
     */
    protected void runAndAssertBenchmark(Class clazz, String testName, TimeUnit timeUnit, AssertionCallback assertions)
    {
        try
        {
            if (getProperty(SKIP_PERFORMANCE_TESTS_SYSTEM_PROPERTY).equals(TRUE.toString()))
            {
                Options opt = new OptionsBuilder()
                        .include(clazz.getSimpleName() + "." + testName)
                        .forks(0)
                        .timeUnit(timeUnit)
                        .warmupIterations(0)
                        .measurementIterations(1)
                        .build();
                new Runner(opt).runSingle();
            }
            else
            {
                Options opt = new OptionsBuilder()
                        .include(clazz.getSimpleName() + "." + testName)
                        .forks(1)
                        .timeUnit(timeUnit)
                        .warmupIterations(10)
                        .measurementIterations(10)
                        .build();
                assertions.assertResult(new Runner(opt).runSingle());
            }
        }
        catch (RunnerException e)
        {
            fail(e.getMessage());
        }
    }

    protected interface AssertionCallback
    {

        void assertResult(RunResult runResult);
    }

}
