/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.message;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public abstract class AbstractPropertyScopeTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Test
    public void testRequestResponse() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage message = getTestMuleMessage();
        message.setOutboundProperty("foo", "fooValue");

        MuleMessage result = sendRequest(client, message);
        assertEquals("test bar", getPayloadAsString(result));
        assertEquals("fooValue", result.<Object> getInboundProperty("foo"));
    }

    protected MuleMessage sendRequest(LocalMuleClient client, MuleMessage message) throws MuleException
    {
        return client.send("inbound", message);
    }
}
