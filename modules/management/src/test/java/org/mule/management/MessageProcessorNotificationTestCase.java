/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.management;

import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.context.notification.MessageProcessorNotification;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class MessageProcessorNotificationTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "message-processor-notification-config.xml";
    }

    @Test
    public void testNotificationPublish() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.send("vm://testInput", TEST_MESSAGE, null);

        MuleMessage notificationResult = client.request("vm://testOut", RECEIVE_TIMEOUT);
        assertTrue(notificationResult.getPayload() instanceof MessageProcessorNotification);
    }
}
