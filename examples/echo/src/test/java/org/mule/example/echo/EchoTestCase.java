/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.echo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.module.http.api.HttpConstants;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.NullPayload;

import org.junit.Rule;
import org.junit.Test;

public class EchoTestCase extends FunctionalTestCase
{
    private static String MESSAGE = "message";

    @Rule
    public DynamicPort port = new DynamicPort("portNumber");

    @Override
    protected String getConfigFile()
    {
        return "adding-logging-to-a-flow.xml";
    }

    @Test
    public void httpGetToFlowUrlEchoesSentMessage() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("http://localhost:" + port.getNumber() +"/" + MESSAGE, new DefaultMuleMessage("", muleContext),
                                         newOptions().method(HttpConstants.Methods.GET).build());
        assertNotNull(result);
        assertFalse(result.getPayload() instanceof NullPayload);
        assertEquals("/" + MESSAGE, result.getPayloadAsString());
    }
}
