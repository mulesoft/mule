/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.functional;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.transport.http.HttpConnector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class HttpBadEncodingFunctionalTestCase extends HttpEncodingFunctionalTestCase
{
    public HttpBadEncodingFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Override
    public void testSend() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        
        // Send as bytes so that the StringRequestEntity isn't used. If it is used
        // it will throw an exception and stop us from testing the server side.
        DefaultMuleMessage msg = new DefaultMuleMessage(TEST_MESSAGE.getBytes(), muleContext);
        msg.setEncoding("UTFF-912");
        MuleMessage reply = client.send("clientEndpoint", msg);
        assertNotNull(reply);
        assertEquals("500", reply.<Object>getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
        assertNotNull(reply.getExceptionPayload());
    }

}
