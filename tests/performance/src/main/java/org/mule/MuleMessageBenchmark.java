/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.mule.BenchmarkUtils.createMuleContext;
import static org.mule.api.transformer.DataType.STRING_DATA_TYPE;
import static org.mule.api.transport.PropertyScope.OUTBOUND;
import static org.openjdk.jmh.annotations.Mode.AverageTime;
import static org.openjdk.jmh.annotations.Scope.Benchmark;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;

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
public class MuleMessageBenchmark
{

    public static final String TEST_PAYLOAD = "test";
    public static final String KEY = "key";
    public static final String VALUE = "value";

    private MuleContext muleContext;
    private MuleMessage message;
    private MuleMessage messageWith20Properties;
    private MuleMessage messageWith100Properties;

    @Setup
    public void setup() throws Exception
    {
        muleContext = createMuleContext();
        message = new DefaultMuleMessage(TEST_PAYLOAD, muleContext);
        messageWith20Properties = new DefaultMuleMessage(TEST_PAYLOAD, muleContext);
        for (int i = 0; i < 20; i++)
        {
            messageWith20Properties.setOutboundProperty(KEY + 1, VALUE);
        }
        messageWith100Properties = new DefaultMuleMessage(TEST_PAYLOAD, muleContext);
        for (int i = 0; i < 100; i++)
        {
            messageWith20Properties.setOutboundProperty(KEY + 1, VALUE);
        }
    }

    @TearDown
    public void teardown()
    {
        muleContext.dispose();
    }

    @Benchmark
    public MuleMessage createMessage() throws MuleException
    {
        return new DefaultMuleMessage(TEST_PAYLOAD, muleContext);
    }

    @Benchmark
    public MuleMessage createMessageWithDataType() throws MuleException
    {
        return new DefaultMuleMessage(TEST_PAYLOAD, null, null, null, muleContext, STRING_DATA_TYPE);
    }

    @Benchmark
    public MuleMessage copyMessage() throws MuleException
    {
        return new DefaultMuleMessage(message);
    }

    @Benchmark
    public MuleMessage copyMessageWith20VariablesProperties() throws MuleException
    {
        return new DefaultMuleMessage(messageWith20Properties);
    }

    @Benchmark
    public MuleMessage copyMessageWith100VariablesProperties() throws MuleException
    {
        return new DefaultMuleMessage(messageWith100Properties);
    }

    @Benchmark
    public MuleMessage mutateMessagePayload() throws MuleException
    {
        message.setPayload(VALUE);
        return message;
    }

    @Benchmark
    public MuleMessage mutateMessagePayloadWithDataType() throws MuleException
    {
        message.setPayload(VALUE, STRING_DATA_TYPE);
        return message;
    }

    @Benchmark
    public MuleMessage addMessageProperty() throws MuleException
    {
        message.setOutboundProperty(KEY, VALUE);
        return message;
    }

    @Benchmark
    public MuleMessage addMessagePropertyMessageWith20Properties() throws MuleException
    {
        messageWith20Properties.setOutboundProperty(KEY, VALUE);
        return message;
    }

    @Benchmark
    public MuleMessage addMessagePropertyMessageWith100Properties() throws MuleException
    {
        messageWith100Properties.setOutboundProperty(KEY, VALUE);
        return message;
    }

    @Benchmark
    public MuleMessage addMessagePropertyWithDataType() throws MuleException
    {
        message.setOutboundProperty(KEY, VALUE, STRING_DATA_TYPE);
        return message;
    }

    @Benchmark
    public MuleMessage addRemoveMessageProperty() throws MuleException
    {
        message.setOutboundProperty(KEY, VALUE);
        message.removeProperty(KEY, OUTBOUND);
        return message;
    }

    @Benchmark
    public MuleMessage addRemoveMessagePropertyMessageWith20Properties() throws MuleException
    {
        messageWith20Properties.setOutboundProperty(KEY, VALUE);
        messageWith20Properties.removeProperty(KEY, OUTBOUND);
        return message;
    }

    @Benchmark
    public MuleMessage addRemoveMessagePropertyMessageWith100Properties() throws MuleException
    {
        messageWith100Properties.setOutboundProperty(KEY, VALUE);
        messageWith100Properties.removeProperty(KEY, OUTBOUND);
        return message;
    }

    @Benchmark
    public MuleMessage addRemoveMessagePropertyWithDataType() throws MuleException
    {
        message.setOutboundProperty(KEY, VALUE, STRING_DATA_TYPE);
        message.removeProperty(KEY, OUTBOUND);
        return message;
    }
}
