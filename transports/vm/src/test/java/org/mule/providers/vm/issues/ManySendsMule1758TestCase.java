/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.vm.issues;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class ManySendsMule1758TestCase extends FunctionalTestCase
{
    private static int NUM_MESSAGES = 100;

    protected String getConfigResources()
    {
        return "many-sends-test.xml";
    }

    public void testSingleSend() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage response = client.send("vm://s-in", "Marco", null);
        assertNotNull("Response is null", response);
        assertEquals("Polo", response.getPayload());
    }

    public void testManySends() throws Exception
    {
        MuleClient client = new MuleClient();
        for (int i = 0; i < NUM_MESSAGES; ++i)
        {
            logger.debug("Message " + i);
            UMOMessage response = client.send("vm://s-in", "Marco", null);
            assertNotNull("Response is null", response);
            assertEquals("Polo", response.getPayload());
        }
    }

}
