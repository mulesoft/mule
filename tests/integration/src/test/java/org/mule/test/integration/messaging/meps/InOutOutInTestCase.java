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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class InOutOutInTestCase extends FunctionalTestCase
{

    public static final long TIMEOUT = 3000;

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/messaging/meps/pattern_In-Out_Out-In.xml";
    }

    @Test
    public void testExchange() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        Map props = new HashMap();
        props.put("foo", "bar");
        MuleMessage result = client.send("inboundEndpoint", "some data", props);
        assertNotNull(result);
        assertEquals("bar header received", result.getPayload());
    }
}
