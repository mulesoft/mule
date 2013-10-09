/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.messaging.meps;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class InOptionalOutOutOptionalInTestCase extends FunctionalTestCase
{

    public static final long TIMEOUT = 3000;

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/messaging/meps/pattern_In-Optional-Out_Out-Optional-In.xml";
    }

    @Test
    public void testExchange() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        MuleMessage result = client.send("inboundEndpoint", "some data", null);
        assertNotNull(result);
        assertEquals(NullPayload.getInstance(), result.getPayload());
        //TODO Even though the component returns a null the remoteSync is honoured.
        // I don't think this is right for Out-Optional-In, but probably should be the behaviour for Out-In
        assertEquals("Received", result.getInboundProperty("externalApp"));

        Map props = new HashMap();
        props.put("foo", "bar");
        result = client.send("inboundEndpoint", "some data", props);
        assertNotNull(result);
        assertEquals("bar header received", result.getPayload());
        assertEquals("Received", result.getInboundProperty("externalApp"));
    }
}
