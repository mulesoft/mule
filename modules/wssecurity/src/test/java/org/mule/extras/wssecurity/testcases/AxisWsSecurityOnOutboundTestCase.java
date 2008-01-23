/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.wssecurity.testcases;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class AxisWsSecurityOnOutboundTestCase extends FunctionalTestCase
{
    public void testAxisWsSecurityOnOutbound() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage message = client.send("vm://testin", new DefaultMuleMessage("Hello World!"));
        assertNotNull(message.getPayload());
        assertTrue(message.getPayloadAsString().startsWith("Hello World!"));
    }  

    protected String getConfigResources()
    {
        return "axis-wssecurity-outbound-with-properties-config.xml";
    }

}


