/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.mule.config.i18n.CoreMessages.agentsRunning;
import static org.openjdk.jmh.annotations.Mode.AverageTime;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleException;
import org.mule.config.i18n.CoreMessages;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

@Fork(1)
@BenchmarkMode(AverageTime)
@OutputTimeUnit(NANOSECONDS)
@Warmup(iterations = 10)
@Measurement(iterations = 10)
public class MessagingExceptionBenchmark
{

    @Benchmark
    @Threads(1)
    public MuleException stringSingleThread()
    {
        return new DefaultMuleException("customMessage");
    }

    @Benchmark
    @Threads(1)
    public MuleException messageSingleThead()
    {
        return new DefaultMuleException(agentsRunning());
    }

    @Benchmark
    @Threads(4)
    public MuleException messageMultiThread()
    {
        return new DefaultMuleException(agentsRunning());
    }

    @Benchmark
    @Threads(4)
    public MuleException stringMultiThread()
    {
        return new DefaultMuleException("customMessage");
    }

}
