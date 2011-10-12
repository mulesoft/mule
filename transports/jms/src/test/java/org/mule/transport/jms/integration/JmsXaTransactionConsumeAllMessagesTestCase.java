/*
 * $Id$
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
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.junit.Assert.assertThat;

/**
 * Testing durable topic with XA transactions
 */
public class JmsXaTransactionConsumeAllMessagesTestCase extends AbstractJmsFunctionalTestCase
{

    public static final String MESSAGE = "some message";
    public static final int TOTAL_MESSAGES = 50;

    @Override
    protected String getConfigResources()
    {
        return "integration/jms-consume-all-messages-tx-xa.xml";
    }

    @Test
    public void testSendSeveralMessagesAndRetrieveThemAll() throws Exception
    {
        MuleClient muleClient = new MuleClient(muleContext);
        FunctionalTestComponent ftc = getFunctionalTestComponent("IncomingMessageConsumer");
        final CountDownLatch allMessageReceived = new CountDownLatch(TOTAL_MESSAGES);
        ftc.setEventCallback(new EventCallback()
        {
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                allMessageReceived.countDown();
            }
        });
        for (int i = 0; i < TOTAL_MESSAGES; i++) {
            muleClient.dispatch("in", MESSAGE, null);
        }
        allMessageReceived.await(3000, TimeUnit.MILLISECONDS);
        for (int i = 0; i < TOTAL_MESSAGES; i++) {
            MuleMessage muleMessage = muleClient.request("out", 300);
            assertThat(muleMessage, IsNull.<Object>notNullValue());
        }
    }
}
