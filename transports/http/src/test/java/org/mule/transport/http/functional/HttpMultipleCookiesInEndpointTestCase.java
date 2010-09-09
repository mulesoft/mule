/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class HttpMultipleCookiesInEndpointTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {

        return "http-multiple-cookies-on-endpoint-test.xml";
    }

    public void testThatThe2CookiesAreSentAndReceivedByTheComponent() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("vm://in", "HELLO", null);
        assertNotNull(response);
        assertNotNull(response.getPayload());
        assertEquals("Both Cookies Found!", response.getPayloadAsString());
    }

}


