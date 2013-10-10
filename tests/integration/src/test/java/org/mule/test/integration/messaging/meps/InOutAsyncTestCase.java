/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.messaging.meps;

import org.junit.runners.Parameterized;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class InOutAsyncTestCase extends AbstractServiceAndFlowTestCase
{

    public static final long TIMEOUT = 3000;

     @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            { ConfigVariant.SERVICE, "org/mule/test/integration/messaging/meps/pattern_In-Out-Async.xml" } ,
            { ConfigVariant.FLOW, "org/mule/test/integration/messaging/meps/pattern_In-Out-Async-flow.xml"}
        });
    }

    public InOutAsyncTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testExchange() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map props = new HashMap();
        //Almost any endpoint can be used here
        props.put(MuleProperties.MULE_REPLY_TO_PROPERTY, "jms://client-reply");

        MuleMessage result = client.send("inboundEndpoint", "some data", props);
        assertNotNull(result);
        assertEquals("got it!", result.getPayloadAsString());

        final Object foo = result.getInboundProperty("foo");
        assertNotNull(foo);
        assertEquals("bar", foo);
    }
}
