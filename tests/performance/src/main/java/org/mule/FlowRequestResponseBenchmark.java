/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static org.mule.BenchmarkUtils.createMuleContext;
import static org.mule.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.MessageExchangePattern.fromString;
import static org.openjdk.jmh.annotations.Mode.AverageTime;
import static org.openjdk.jmh.annotations.Scope.Benchmark;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextFactory;
import org.mule.api.processor.MessageProcessor;
import org.mule.construct.Flow;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.processor.strategy.SynchronousProcessingStrategy;
import org.mule.tck.TriggerableMessageSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

@State(Benchmark)
@Fork(1)
@BenchmarkMode(AverageTime)
@OutputTimeUnit(MICROSECONDS)
@Warmup(iterations = 10)
@Measurement(iterations = 10)
public class FlowRequestResponseBenchmark
{

    public static final String TEST_PAYLOAD = "test";

    private MuleContext muleContext;
    private Flow flow;
    private TriggerableMessageSource source;

    @Setup
    public void setup() throws Exception
    {
        muleContext = createMuleContext();
        muleContext.start();

        source = new TriggerableMessageSource();
        flow = new Flow("flow", muleContext);
        List<MessageProcessor> processors = new ArrayList<>();
        flow.setMessageProcessors(Collections.<MessageProcessor>singletonList(new MessageProcessor()
        {

            @Override
            public MuleEvent process(MuleEvent event)
            {
                return event;
            }
        }));
        flow.setMessageSource(source);
        flow.setProcessingStrategy(new SynchronousProcessingStrategy());
        muleContext.getRegistry().registerFlowConstruct(flow);
    }

    @TearDown
    public void teardown()
    {
        muleContext.dispose();
    }

    @Benchmark
    @Threads(1)
    public MuleEvent processSourceRequestResponse1Thread() throws MuleException
    {
        return source.trigger(new DefaultMuleEvent(new DefaultMuleMessage(TEST_PAYLOAD, muleContext), REQUEST_RESPONSE, flow));
    }

    @Benchmark
    @Threads(2)
    public MuleEvent processSourceRequestResponse2Threads() throws MuleException
    {
        return source.trigger(new DefaultMuleEvent(new DefaultMuleMessage(TEST_PAYLOAD, muleContext), REQUEST_RESPONSE, flow));
    }

    @Benchmark
    @Threads(4)
    public MuleEvent processSourceRequestResponse4Threads() throws MuleException
    {
        return source.trigger(new DefaultMuleEvent(new DefaultMuleMessage(TEST_PAYLOAD, muleContext), REQUEST_RESPONSE, flow));
    }

    @Benchmark
    @Threads(8)
    public MuleEvent processSourceRequestResponse8Threads() throws MuleException
    {
        return source.trigger(new DefaultMuleEvent(new DefaultMuleMessage(TEST_PAYLOAD, muleContext), REQUEST_RESPONSE, flow));
    }

    @Benchmark
    @Threads(16)
    public MuleEvent processSourceRequestResponse16Threads() throws MuleException
    {
        return source.trigger(new DefaultMuleEvent(new DefaultMuleMessage(TEST_PAYLOAD, muleContext), REQUEST_RESPONSE, flow));
    }

    @Benchmark
    @Threads(32)
    public MuleEvent processSourceRequestResponse32Threads() throws MuleException
    {
        return source.trigger(new DefaultMuleEvent(new DefaultMuleMessage(TEST_PAYLOAD, muleContext), REQUEST_RESPONSE, flow));
    }

    @Benchmark
    @Threads(64)
    public MuleEvent processSourceRequestResponse64Threads() throws MuleException
    {
        return source.trigger(new DefaultMuleEvent(new DefaultMuleMessage(TEST_PAYLOAD, muleContext), REQUEST_RESPONSE, flow));
    }

    @Benchmark
    @Threads(128)
    public MuleEvent processSourceRequestResponse128Threads() throws MuleException
    {
        return source.trigger(new DefaultMuleEvent(new DefaultMuleMessage(TEST_PAYLOAD, muleContext), REQUEST_RESPONSE, flow));
    }

    @Benchmark
    @Threads(256)
    public MuleEvent processSourceRequestResponse256Threads() throws MuleException
    {
        return source.trigger(new DefaultMuleEvent(new DefaultMuleMessage(TEST_PAYLOAD, muleContext), REQUEST_RESPONSE, flow));
    }

}
