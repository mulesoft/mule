/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.messaging.meps;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.service.Service;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class InOptionalOutOutOnlyAsyncRouterTestCase extends FunctionalTestCase
{

    public static final long TIMEOUT = 3000;

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/messaging/meps/pattern_In-Optional-Out_Out-Only-Async-Router.xml";
    }

    @Test
    public void testExchange() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        MuleMessage result = client.send("inboundEndpoint", "some data", null);
        assertNotNull(result);
        assertEquals(NullPayload.getInstance(), result.getPayload());
        assertNull(result.getExceptionPayload());

        DefaultMuleMessage msg = new DefaultMuleMessage("some data", muleContext);
        msg.setOutboundProperty("foo", "bar");
        result = client.send("inboundEndpoint", msg);
        assertNotNull(result);
        assertEquals("got it!", result.getPayloadAsString());

        Service async = muleContext.getRegistry().lookupService("In-Out_Out-Only-Async-Service");
        Service external = muleContext.getRegistry().lookupService("ExternalApp");

        assertEquals(2, async.getStatistics().getProcessedEvents());
        assertEquals(1, external.getStatistics().getProcessedEvents());
    }
}
