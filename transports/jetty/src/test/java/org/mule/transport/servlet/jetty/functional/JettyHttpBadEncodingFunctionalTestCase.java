/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.transport.http.HttpConnector;

public class JettyHttpBadEncodingFunctionalTestCase extends JettyHttpEncodingFunctionalTestCase
{
    public JettyHttpBadEncodingFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Override
    public void testSend() throws Exception
    {
        MuleClient client = muleContext.getClient();

        // Send as bytes so that the StringRequestEntity isn't used. If it is used
        // it will throw an exception and stop us from testing the server side.
        DefaultMuleMessage msg = new DefaultMuleMessage(TEST_MESSAGE.getBytes(), muleContext);
        msg.setEncoding("UTFF-912");
        MuleMessage reply = client.send("clientEndpoint", msg);
        assertNotNull(reply);
        assertEquals("500", reply.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
        assertNotNull(reply.getExceptionPayload());
    }
}
