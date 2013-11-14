/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
    protected String getConfigFile()
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
