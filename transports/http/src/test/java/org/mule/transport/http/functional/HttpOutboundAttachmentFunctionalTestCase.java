/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;


import static org.junit.Assert.assertEquals;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class HttpOutboundAttachmentFunctionalTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort httpPort = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "http-outbound-attachments-config.xml";
    }

    @Test
    public void sendsStringAttachmentCorrectly() throws Exception
    {
        sendMessageAndAssertResponse("vm://inString");
    }

    @Test
    public void sendsByteArrayAttachmentCorrectly() throws Exception
    {
        sendMessageAndAssertResponse("vm://inByteArray");
    }


    private void sendMessageAndAssertResponse(String endpoint) throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage answer = client.send(endpoint, TEST_MESSAGE, null);
        assertEquals(TEST_MESSAGE, answer.getPayloadAsString());
    }
}
