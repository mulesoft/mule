/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.messaging.meps;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

// START SNIPPET: full-class
public class InOnlyOptionalOutTestCase extends AbstractServiceAndFlowTestCase
{
    public static final long TIMEOUT = 3000;

    public InOnlyOptionalOutTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE,
                "org/mule/test/integration/messaging/meps/pattern_In-Only_Optional-Out-service.xml"},
            {ConfigVariant.FLOW,
                "org/mule/test/integration/messaging/meps/pattern_In-Only_Optional-Out-flow.xml"}});
    }

    @Test
    public void testExchange() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

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
