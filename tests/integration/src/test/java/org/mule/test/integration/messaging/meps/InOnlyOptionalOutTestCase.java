/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.messaging.meps;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

import java.util.HashMap;
import java.util.Map;

// START SNIPPET: full-class
public class InOnlyOptionalOutTestCase extends FunctionalTestCase
{
    public static final long TIMEOUT = 3000;

    protected String getConfigResources()
    {
        return "org/mule/test/integration/messaging/meps/pattern_In-Only_Optional-Out.xml";
    }

    public void testExchange() throws Exception
    {
        MuleClient client = new MuleClient();

        client.dispatch("inboundEndpoint", "some data", null);
        Map props = new HashMap();
        props.put("foo", "bar");
        client.dispatch("inboundEndpoint", "some data", props);

        MuleMessage result = client.request("receivedEndpoint", TIMEOUT);
        assertNotNull(result);
        assertEquals("foo header received", result.getPayloadAsString());

        result = client.request("notReceivedEndpoint", TIMEOUT);
        assertNull(result);
    }
}
// END SNIPPET: full-class