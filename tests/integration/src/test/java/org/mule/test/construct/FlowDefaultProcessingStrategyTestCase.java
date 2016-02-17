/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.PropertyScope;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.functional.junit4.TransactionConfigEnum;
import org.mule.tck.testmodels.mule.TestTransactionFactory;

import org.junit.Test;

public class FlowDefaultProcessingStrategyTestCase extends FunctionalTestCase
{

    protected static final String PROCESSOR_THREAD = "processor-thread";
    protected static final String FLOW_NAME = "Flow";

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/construct/flow-default-processing-strategy-config.xml";
    }

    @Test
    public void requestResponse() throws Exception
    {
        MuleMessage response = flowRunner(FLOW_NAME).withPayload(TEST_PAYLOAD).run().getMessage();
        assertThat(response.getPayload().toString(), is(TEST_PAYLOAD));
        MuleMessage message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT);
        assertThat(message.getProperty(PROCESSOR_THREAD, PropertyScope.OUTBOUND), is(Thread.currentThread().getName()));
    }

    @Test
    public void oneWay() throws Exception
    {
        flowRunner(FLOW_NAME).withPayload(TEST_PAYLOAD).asynchronously().run();
        MuleMessage message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT);
        assertThat(message.getProperty(PROCESSOR_THREAD, PropertyScope.OUTBOUND), is(not(Thread.currentThread().getName())));
    }

    @Test
    public void requestResponseTransacted() throws Exception
    {
        flowRunner("Flow").withPayload(TEST_PAYLOAD)
                          .transactionally(TransactionConfigEnum.ACTION_NONE, new TestTransactionFactory())
                          .run();

        MuleMessage message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT);
        assertThat(message.getProperty(PROCESSOR_THREAD, PropertyScope.OUTBOUND), is(Thread.currentThread().getName()));
    }

    @Test
    public void oneWayTransacted() throws Exception
    {
        flowRunner("Flow").withPayload(TEST_PAYLOAD)
                          .transactionally(TransactionConfigEnum.ACTION_NONE, new TestTransactionFactory())
                          .asynchronously()
                          .run();

        MuleMessage message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT);
        assertThat(message.getProperty(PROCESSOR_THREAD, PropertyScope.OUTBOUND), is(Thread.currentThread().getName()));
    }

    protected void testTransacted(MessageExchangePattern mep) throws Exception
    {
        flowRunner("Flow").withPayload(TEST_PAYLOAD)
                          .transactionally(TransactionConfigEnum.ACTION_NONE, new TestTransactionFactory())
                          .run();

        MuleMessage message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT);
        assertThat(message.getProperty(PROCESSOR_THREAD, PropertyScope.OUTBOUND), is(Thread.currentThread().getName()));
    }


    public static class ThreadSensingMessageProcessor implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            event.getMessage().setOutboundProperty(PROCESSOR_THREAD, Thread.currentThread().getName());
            return event;
        }
    }

}
