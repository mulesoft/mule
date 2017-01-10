/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import org.mule.el.mvel.MVELBenchmark;
import org.mule.el.mvel.MVELDeepAssignBenchmark;
import org.mule.el.mvel.MVELDeepInvokeBenchmark;

import org.junit.Test;

public class MVELBenchmarkAssertionTestCase extends AbstractBenchmarkAssertionTestCase
{

    @Test
    public void mvelColdStart()
    {
        runAndAssertBenchmark(MVELBenchmark.class, "mvelColdStart", 1000, MICROSECONDS);
    }

    @Test
    public void mvelWarmStart()
    {
        runAndAssertBenchmark(MVELBenchmark.class, "mvelWarmStart", 10, MICROSECONDS);
    }

    @Test
    public void mvelHotStart()
    {
        runAndAssertBenchmark(MVELBenchmark.class, "mvelHotStart", 10, MICROSECONDS);
    }

    @Test
    public void mvelColdStartDeepAssign()
    {
        runAndAssertBenchmark(MVELDeepAssignBenchmark.class, "mvelColdStart", 3000, MICROSECONDS);
    }

    @Test
    public void mvelWarmStartDeepAssign()
    {
        runAndAssertBenchmark(MVELDeepAssignBenchmark.class, "mvelWarmStart", 2, MICROSECONDS);
    }

    @Test
    public void mvelHotStartDeepAssign()
    {
        runAndAssertBenchmark(MVELDeepAssignBenchmark.class, "mvelHotStart", 2, MICROSECONDS);
    }

    @Test
    public void mvelColdStartDeepInvoke()
    {
        runAndAssertBenchmark(MVELDeepInvokeBenchmark.class, "mvelColdStart", 600, MICROSECONDS);
    }

    @Test
    public void mvelWarmStartDeepInvoke()
    {
        runAndAssertBenchmark(MVELDeepInvokeBenchmark.class, "mvelWarmStart", 2, MICROSECONDS);
    }

    @Test
    public void mvelHotStartDeepInvoke()
    {
        runAndAssertBenchmark(MVELDeepInvokeBenchmark.class, "mvelHotStart", 2, MICROSECONDS);
    }

}
