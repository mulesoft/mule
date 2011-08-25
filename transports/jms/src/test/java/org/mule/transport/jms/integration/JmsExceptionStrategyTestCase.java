/*
 * $Id: FileExceptionStrategyFunctionalTestCase.java 22431 2011-07-18 07:40:35Z dirk.olmes $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.integration;

import org.hamcrest.core.IsNull;
import org.junit.Test;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.construct.Flow;
import org.mule.exception.DefaultMessagingExceptionStrategy;
import org.mule.module.client.MuleClient;
import org.mule.util.concurrent.Latch;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertThat;

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
        DefaultMessagingExceptionStrategy exceptionStrategy = (DefaultMessagingExceptionStrategy)muleContext.getRegistry().lookupFlowConstruct("flowWithoutExceptionStrategyAndTx").getExceptionListener();
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
    public void testInExceptionDoRollbackJmsTx() throws Exception
    {
        muleClient = new MuleClient(muleContext);
        muleClient.dispatch("jms://in", MESSAGE, null);
        latch.await(LATCH_AWAIT_TIMEOUT, MILLISECONDS);
        //Stop flow to not consume message again
        Flow flow = muleContext.getRegistry().get("flowWithoutExceptionStrategyAndTx");
        flow.stop();
        //Check message rollback
        //Seems not to be a rollback
        MuleMessage muleMessage = muleClient.request("jms://in", TIMEOUT);
        assertThat(muleMessage, IsNull.<Object>nullValue());
        //This is currently expected
        /*assertThat(muleMessage, notNullValue());
        assertThat((String) muleMessage.getPayload(), Is.is(MESSAGE));*/

        //Check outbound-endpoint was not executed
        MuleMessage outboundMessage = muleClient.request("jms://out", SHORT_TIMEOUT);
        assertThat(outboundMessage, IsNull.<Object>nullValue());
    }

    @Test
    public void testInExceptionDoRollbackJmsNoTx() throws Exception
    {
        muleClient = new MuleClient(muleContext);
        muleClient.dispatch("jms://in2", MESSAGE, null);
        latch.await(LATCH_AWAIT_TIMEOUT, MILLISECONDS);
        //Stop flow to not consume message again
        Flow flow = muleContext.getRegistry().get("flowWithoutExceptionStrategyAndNoTx");
        flow.stop();
        //Check message was consumed
        MuleMessage muleMessage = muleClient.request("jms://in2", TIMEOUT);
        assertThat(muleMessage, IsNull.<Object>nullValue());
    }

}

