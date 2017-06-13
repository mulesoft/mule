/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.mule.BenchmarkUtils.createMuleContext;
import static org.mule.MessageExchangePattern.ONE_WAY;
import static org.openjdk.jmh.annotations.Mode.AverageTime;
import static org.openjdk.jmh.annotations.Scope.Benchmark;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

@State(Benchmark)
@Fork(1)
@Threads(1)
@BenchmarkMode(AverageTime)
@OutputTimeUnit(NANOSECONDS)
@Warmup(iterations = 10)
@Measurement(iterations = 10)
public class ExpressionBenchmark
{

    public static final String TEST_PAYLOAD = "test";

    private MuleContext muleContext;
    private Flow flow;
    private MuleEvent event;

    @Setup
    public void setup() throws Exception
    {
        muleContext = createMuleContext();
        muleContext.start();

        flow = new Flow("flow", muleContext);
        muleContext.getRegistry().registerFlowConstruct(flow);
        event = new DefaultMuleEvent(new DefaultMuleMessage(TEST_PAYLOAD, muleContext), ONE_WAY, flow);
        event.setFlowVariable("foo", "bar");
    }

    @TearDown
    public void teardown()
    {
        muleContext.dispose();
    }

    @Benchmark
    public Object nullExpression()
    {
        return muleContext.getExpressionManager().evaluate("flowVars['dummy']", event);
    }

    @Benchmark

    public Object payloadExpression()
    {
        return muleContext.getExpressionManager().evaluate("payload", event);
    }

    @Benchmark
    public Object melFlowVars()
    {
        return muleContext.getExpressionManager().evaluate("flowVars['foo']=='bar'", event);
    }

    @Benchmark
    public Object dwFlowVars()
    {
        return muleContext.getExpressionManager().evaluate("variables.foo == 'bar'", event);
    }
}
