/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.message;

import org.junit.Test;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.DynamicPortTestCase;

/**
 * TODO
 */
public abstract class AbstractPropertyScopeTestCase extends DynamicPortTestCase
{
    public AbstractPropertyScopeTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        
    }
    
    @Test
    public void testRequestResponse() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage message = new DefaultMuleMessage("test", muleContext);
        message.setOutboundProperty("foo", "fooValue");

        MuleMessage result = client.send("inbound", message);
        assertEquals("test bar", result.getPayloadAsString());
        assertEquals("fooValue", result.<Object> getInboundProperty("foo"));
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 1;
    }
}
