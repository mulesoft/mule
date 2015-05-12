/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.integration;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.construct.SimpleFlowConstruct;
import org.mule.exception.DefaultServiceExceptionStrategy;
import org.mule.module.client.MuleClient;
import org.mule.util.concurrent.Latch;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Ignore;
import org.junit.Test;

public class JmsExceptionStrategyTestCase extends AbstractJmsFunctionalTestCase
{

    public static final String MESSAGE = "some message";
    public static final int TIMEOUT = 3000;
    public static final int SHORT_TIMEOUT = 500;
    private Latch latch;
    private MuleClient muleClient;
    private static final long LATCH_AWAIT_TIMEOUT = 3000;


    @Override
    protected String getConfigResources()
    {
        return "integration/jms-exception-strategy.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        latch = new Latch();
        muleClient = new MuleClient(muleContext);
        DefaultServiceExceptionStrategy exceptionStrategy = (DefaultServiceExceptionStrategy)muleContext.getRegistry().lookupFlowConstruct("flowWithoutExceptionStrategyAndTx").getExceptionListener();
        exceptionStrategy.getMessageProcessors().add(new MessageProcessor()
        {
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                latch.countDown();
                return event;
            }
        });
    }

    @Test
    @Ignore("MULE-6926: Flaky test")
    public void testInExceptionDoRollbackJmsTx() throws Exception
    {
        muleClient = new MuleClient(muleContext);
        muleClient.dispatch("jms://in", MESSAGE, null);
        latch.await(LATCH_AWAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        //Stop flow to not consume message again
        SimpleFlowConstruct simpleFlowConstruct = muleContext.getRegistry().get("flowWithoutExceptionStrategyAndTx");
        simpleFlowConstruct.stop();
        //Check message rollback
        MuleMessage muleMessage = muleClient.request("jms://in", TIMEOUT);
        assertThat(muleMessage, notNullValue());
        assertThat((String) muleMessage.getPayload(), Is.is(MESSAGE));
        //Check outbound-endpoint was not executed
        MuleMessage outboundMessage = muleClient.request("jms://out", SHORT_TIMEOUT);
        assertThat(outboundMessage, IsNull.<Object>nullValue());
    }

    @Test
    public void testInExceptionDoRollbackJmsNoTx() throws Exception
    {
        muleClient = new MuleClient(muleContext);
        muleClient.dispatch("jms://in2", MESSAGE, null);
        latch.await(LATCH_AWAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        //Stop flow to not consume message again
        SimpleFlowConstruct simpleFlowConstruct = muleContext.getRegistry().get("flowWithoutExceptionStrategyAndNoTx");
        simpleFlowConstruct.stop();
        //Check message was consumed
        MuleMessage muleMessage = muleClient.request("jms://in2", TIMEOUT);
        assertThat(muleMessage, IsNull.<Object>nullValue());

        //Check outbound-endpoint was not executed
        MuleMessage outboundMessage = muleClient.request("jms://out2", SHORT_TIMEOUT);
        assertThat(outboundMessage, IsNull.<Object>nullValue());
    }
    
    @Test
    public void testDefaultStrategyConfigured() throws Exception
    {
        muleClient = new MuleClient(muleContext);
        muleClient.dispatch("jms://in3", MESSAGE, null);
        latch.await(LATCH_AWAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        //Stop flow to not consume message again
        SimpleFlowConstruct flow = muleContext.getRegistry().get("flowWithDefaultStrategyConfigured");
        flow.stop();
        //Check message was no consumed
        MuleMessage muleMessage = muleClient.request("jms://in3", TIMEOUT);
        assertThat(muleMessage, IsNull.<Object>nullValue());
        
        //Check outbound-endpoint was not executed
        MuleMessage outboundMessage = muleClient.request("jms://out3", SHORT_TIMEOUT);
        assertThat(outboundMessage, IsNull.<Object>nullValue());
    }
    
    @Test
    public void testSendExceptionNofication() throws Exception
    {
        muleClient = new MuleClient(muleContext);
        muleClient.dispatch("jms://in4", MESSAGE, null);
        latch.await(LATCH_AWAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        //Stop flow to not consume message again
        SimpleFlowConstruct flow = muleContext.getRegistry().get("flowWithExceptionNotification");
        flow.stop();
        //Check message was no consumed
        MuleMessage muleMessage = muleClient.request("jms://in4", TIMEOUT);
        assertThat(muleMessage, IsNull.<Object>nullValue());

        // Check exception notification was sent
        MuleMessage exceptionMessage = muleClient.request("jms://exception4", TIMEOUT);
        assertThat(exceptionMessage, IsNull.<Object> notNullValue());
        assertThat(exceptionMessage.getPayload(), IsNull.<Object> notNullValue());
        
        //Check outbound-endpoint was not executed
        MuleMessage outboundMessage = muleClient.request("jms://out4", SHORT_TIMEOUT);
        assertThat(outboundMessage, IsNull.<Object>nullValue());
    }

    @Test
    public void testFlowConfiguredForDeadLetterQueue() throws Exception
    {
        muleClient = new MuleClient(muleContext);
        muleClient.dispatch("jms://in5", MESSAGE, null);
        latch.await(LATCH_AWAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        //Stop flow to not consume message again
        SimpleFlowConstruct flow = muleContext.getRegistry().get("flowConfiguredForDeadLetterQueue");
        flow.stop();
        //Check message was no consumed
        MuleMessage muleMessage = muleClient.request("jms://in5", TIMEOUT);
        assertThat(muleMessage, IsNull.<Object>nullValue());

        // Check exception notification was sent
        MuleMessage deadLetter = muleClient.request("jms://DLQ5", TIMEOUT);
        assertThat(deadLetter, IsNull.<Object> notNullValue());
        assertThat(deadLetter.getPayload(), IsNull.<Object> notNullValue());
        
        //Check outbound-endpoint was not executed
        MuleMessage outboundMessage = muleClient.request("jms://out5", SHORT_TIMEOUT);
        assertThat(outboundMessage, IsNull.<Object>nullValue());
    }

    @Test
    public void testFlowConfiguredForDeadLetterQueueTx() throws Exception
    {
        muleClient = new MuleClient(muleContext);
        muleClient.dispatch("jms://in6", MESSAGE, null);
        latch.await(LATCH_AWAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        //Stop flow to not consume message again
        SimpleFlowConstruct flow = muleContext.getRegistry().get("flowConfiguredForDeadLetterQueueTx");
        flow.stop();
        //Check message was no consumed
        MuleMessage muleMessage = muleClient.request("jms://in6", TIMEOUT);
        assertThat(muleMessage, IsNull.<Object>nullValue());

        // Check exception notification was sent
        MuleMessage deadLetter = muleClient.request("jms://DLQ6", TIMEOUT);
        assertThat(deadLetter, IsNull.<Object> notNullValue());
        assertThat(deadLetter.getPayload(), IsNull.<Object> notNullValue());

        //Check outbound-endpoint was not executed
        MuleMessage outboundMessage = muleClient.request("jms://out6", SHORT_TIMEOUT);
        assertThat(outboundMessage, IsNull.<Object>nullValue());
    }


}

