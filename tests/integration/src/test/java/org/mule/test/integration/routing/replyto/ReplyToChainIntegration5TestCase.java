/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.replyto;

import static org.junit.Assert.assertThat;
import org.mule.functional.functional.EventCallback;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.functional.junit4.FunctionalTestCase;

import java.util.concurrent.TimeUnit;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Test;

public class ReplyToChainIntegration5TestCase extends FunctionalTestCase
{

    public static final String TEST_PAYLOAD = "test payload";
    public static final String EXPECTED_PAYLOAD = TEST_PAYLOAD + " modified";
    public static final int TIMEOUT = 5000;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/replyto/replyto-chain-integration-test-5-flow.xml";
    }

    @Test
    public void testReplyToIsHonoredInFlowUsingAsyncBlock() throws Exception
    {
        org.mule.api.client.LocalMuleClient client = muleContext.getClient();
        final org.mule.util.concurrent.Latch flowExecutedLatch = new org.mule.util.concurrent.Latch();
        FunctionalTestComponent ftc = getFunctionalTestComponent("replierService");
        ftc.setEventCallback(new EventCallback()
        {
            @Override
            public void eventReceived(org.mule.api.MuleEventContext context, Object component) throws Exception
            {
                flowExecutedLatch.release();
            }
        });
        org.mule.api.MuleMessage muleMessage = new org.mule.DefaultMuleMessage(TEST_PAYLOAD, muleContext);
        muleMessage.setOutboundProperty(org.mule.api.config.MuleProperties.MULE_REPLY_TO_PROPERTY,"jms://response");
        client.dispatch("jms://jmsIn1", muleMessage);
        flowExecutedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS);
        org.mule.api.MuleMessage response = client.request("jms://response", TIMEOUT);
        assertThat(response, IsNull.<Object>notNullValue());
        assertThat(getPayloadAsString(response), Is.is(EXPECTED_PAYLOAD));
    }
}
