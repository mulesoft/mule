/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.vm;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class VMFunctionalTestCase extends FunctionalTestCase
{

    public static final long WAIT = 3000L;

    protected String getConfigResources()
    {
        return "vm/vm-functional-test.xml";
    }

    public void testSingleMessage() throws Exception
    {
        MuleClient client = new MuleClient();
        client.dispatch("vm://in", "Marco", null);
        UMOMessage response = client.receive("vm://out", WAIT);
        assertNotNull("Response is null", response);
        assertEquals("Polo", response.getPayload());
    }

    public void testRequest() throws Exception
    {
        MuleClient client = new MuleClient();
        client.dispatch("vm://in", "Marco", null);
        UMOMessage response = client.request("vm://out", WAIT);
        assertNotNull("Response is null", response);
        assertEquals("Polo", response.getPayload());
    }

    public void testMultipleMessages() throws Exception
    {
        MuleClient client = new MuleClient();
        client.dispatch("vm://in", "Marco", null);
        client.dispatch("vm://in", "Marco", null);
        client.dispatch("vm://in", "Marco", null);
        UMOMessage response;
        for (int i = 0; i < 3; ++i)
        {
            response = client.receive("vm://out", WAIT);
            assertNotNull("Response is null", response);
            assertEquals("Polo", response.getPayload());
        }
    }
}
