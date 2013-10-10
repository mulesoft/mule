/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.message;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VmPropertyScopeTestCase extends AbstractPropertyScopeTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/message/vm-property-scope.xml";
    }

    @Test
    public void testRequestResponseChain() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage message = new DefaultMuleMessage("test", muleContext);
        message.setOutboundProperty("foo", "fooValue");

        MuleMessage result = client.send("inbound2", message);
        assertEquals("test bar", result.getPayload());
        assertEquals("fooValue", result.<Object>getInboundProperty("foo"));
    }
}
