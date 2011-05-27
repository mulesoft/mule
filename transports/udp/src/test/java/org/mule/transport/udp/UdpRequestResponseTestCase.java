/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.udp;

import org.mule.api.MuleMessage;
import org.mule.tck.DynamicPortTestCase;

public class UdpRequestResponseTestCase extends DynamicPortTestCase
{
    private static final String EXPECTED = TEST_MESSAGE + " received";

    @Override
    protected String getConfigResources()
    {
        return "udp-request-response.xml";
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 1;
    }

    public void testRequestResponse() throws Exception
    {
        MuleMessage response = muleContext.getClient().send("vm://fromTest", TEST_MESSAGE, null);
        assertEquals(EXPECTED, response.getPayloadAsString());
    }
}
