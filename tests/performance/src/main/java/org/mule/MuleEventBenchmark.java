/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.mule.BenchmarkUtils.createMuleContext;
import static org.mule.MessageExchangePattern.ONE_WAY;
import static org.openjdk.jmh.annotations.Mode.AverageTime;
import static org.openjdk.jmh.annotations.Scope.Benchmark;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
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
public class MuleEventBenchmark
{

    public static final String TEST_PAYLOAD = "test";
    public static final String KEY = "key";
    public static final String VALUE = "value";

    private MuleContext muleContext;
    private Flow flow;
    private MuleEvent event;
    private MuleEvent eventWith20VariablesProperties;
    private MuleEvent eventWith100VariablesProperties;

    @Setup
    public void setup() throws Exception
    {
        muleContext = createMuleContext();
        muleContext.start();

        flow = new Flow("flow", muleContext);
        muleContext.getRegistry().registerFlowConstruct(flow);
        event = new DefaultMuleEvent(new DefaultMuleMessage(TEST_PAYLOAD, muleContext), ONE_WAY, flow);
        eventWith20VariablesProperties = new DefaultMuleEvent(new DefaultMuleMessage(TEST_PAYLOAD, muleContext),
                                                              ONE_WAY, flow);
        for (int i = 0; i < 20; i++)
        {
            eventWith20VariablesProperties.setFlowVariable(KEY + i, VALUE);
            eventWith20VariablesProperties.getMessage().setOutboundProperty(KEY + 1, VALUE);
        }
        eventWith100VariablesProperties = new DefaultMuleEvent(new DefaultMuleMessage(TEST_PAYLOAD, muleContext),
                                                              ONE_WAY, flow);
        for (int i = 0; i < 100; i++)
        {
            eventWith100VariablesProperties.setFlowVariable(KEY + i, VALUE);
            eventWith100VariablesProperties.getMessage().setOutboundProperty(KEY + 1, VALUE);
        }
    }

    @TearDown
    public void teardown()
    {
        muleContext.dispose();
    }

    @Benchmark
    public String eventUUID() throws MuleException
    {
        return muleContext.getUniqueIdString();
    }

    @Benchmark
    public MuleEvent createEvent() throws MuleException
    {
        return new DefaultMuleEvent(new DefaultMuleMessage(TEST_PAYLOAD, muleContext), ONE_WAY, flow);
    }

    @Benchmark
    public MuleEvent copyEvent() throws MuleException
    {
        return new DefaultMuleEvent(event.getMessage(), event);
    }

    @Benchmark
    public MuleEvent copyEventWith20VariablesProperties() throws MuleException
    {
        return new DefaultMuleEvent(eventWith20VariablesProperties.getMessage(), eventWith20VariablesProperties);
    }

    @Benchmark
    public MuleEvent copyEventWith100VariablesProperties() throws MuleException
    {
        return new DefaultMuleEvent(eventWith100VariablesProperties.getMessage(), eventWith100VariablesProperties);
    }

    @Benchmark
    public MuleEvent deepCopyEvent() throws MuleException
    {
        return DefaultMuleEvent.copy(event);
    }

    @Benchmark
    public MuleEvent deepCopyEventWith20VariablesProperties() throws MuleException
    {
        return DefaultMuleEvent.copy(eventWith20VariablesProperties);
    }

    @Benchmark
    public MuleEvent deepCopyEventWith100VariablesProperties() throws MuleException
    {
        return DefaultMuleEvent.copy(eventWith100VariablesProperties);
    }

    @Benchmark
    public MuleEvent addEventVariable() throws MuleException
    {
        event.setFlowVariable(KEY, VALUE);
        return event;
    }

    @Benchmark
    public MuleEvent addEventVariableEventWith20VariablesProperties() throws MuleException
    {
        eventWith20VariablesProperties.setFlowVariable(KEY, VALUE);
        return event;
    }

    @Benchmark
    public MuleEvent addEventVariableEventWith100VariablesProperties() throws MuleException
    {
        eventWith100VariablesProperties.setFlowVariable(KEY, VALUE);
        return event;
    }

}
