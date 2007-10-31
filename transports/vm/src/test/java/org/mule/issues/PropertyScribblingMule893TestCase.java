/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.issues;

import org.mule.config.MuleProperties;
import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.util.HashMap;
import java.util.Map;

public class PropertyScribblingMule893TestCase extends FunctionalTestCase
{
    MuleClient client = null;

    protected String getConfigResources()
    {
        return "issues/property-scribbling-mule-893-test.xml";
    }

    public void testSingleMessage() throws Exception
    {
        if (client == null)
        {
            client = new MuleClient();
        }
        Map properties = new HashMap();
        properties.put(MuleProperties.MULE_REPLY_TO_PROPERTY, "receive");
        client.dispatch("dispatch", "Message", properties);
        UMOMessage response = client.receive("receive", 3000L);
        assertNotNull("Response is null", response);
        assertEquals("Message Received", response.getPayload());
    }

    public void testManyMessage() throws Exception
    {
        int NUM_MESSAGES = 1000;
        int msg = 0;
        try
        {
            while (msg < NUM_MESSAGES)
            {
                testSingleMessage();
                msg++;
            }
        }
        finally 
        {
            logger.info(msg + " messages out of " + NUM_MESSAGES + " processed sucessfully.");
        }
    }

}
