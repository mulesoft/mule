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

/**
 * Simple synch test used to study message flow.
 */
public class VMSynchTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "vm/vm-synch-test.xml";
    }

    public void testSingleMessage() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage response =  client.send("vm://bridge", "Message", null);
        assertNotNull("Response is null", response);
        assertEquals("Message Received", response.getPayload());
    }

    public void testManyMessage() throws Exception
    {
        for (int i = 0; i < 1000; i++)
        {
            testSingleMessage();
        }
    }

}