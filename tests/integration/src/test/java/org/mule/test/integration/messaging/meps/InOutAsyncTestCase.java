/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.messaging.meps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.config.MuleProperties;
import org.mule.functional.junit4.FunctionalTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class InOutAsyncTestCase extends FunctionalTestCase
{
    public static final long TIMEOUT = 3000;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/messaging/meps/pattern_In-Out-Async-flow.xml";
    }

    @Test
    public void testExchange() throws Exception
    {
        MuleClient client = muleContext.getClient();

        Map<String, Object> props = new HashMap<String, Object>();
        //Almost any endpoint can be used here
        props.put(MuleProperties.MULE_REPLY_TO_PROPERTY, "jms://client-reply");

        MuleMessage result = client.send("inboundEndpoint", "some data", props);
        assertNotNull(result);
        assertEquals("got it!", getPayloadAsString(result));

        final Object foo = result.getInboundProperty("foo");
        assertNotNull(foo);
        assertEquals("bar", foo);
    }
}
