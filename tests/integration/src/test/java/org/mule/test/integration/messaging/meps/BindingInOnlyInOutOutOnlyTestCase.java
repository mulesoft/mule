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

// START SNIPPET: full-class
public class BindingInOnlyInOutOutOnlyTestCase extends FunctionalTestCase
{
    public static final long TIMEOUT = 3000;

    protected String getConfigResources()
    {
        return "org/mule/test/integration/messaging/meps/pattern_binding-In-Only_In-Out_Out-Only.xml";
    }

    public void testExchange() throws Exception
    {
        MuleClient client = new MuleClient();


        client.dispatch("inboundEndpoint", new int[]{1,2,3,4,5}, null);

        MuleMessage result = client.request("receivedEndpoint", TIMEOUT);
        assertNotNull(result);
        assertEquals("Total: 15", result.getPayloadAsString());
    }
}
// END SNIPPET: full-class