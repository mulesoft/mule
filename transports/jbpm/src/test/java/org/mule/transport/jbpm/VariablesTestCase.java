/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jbpm;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.transport.bpm.BPMS;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class VariablesTestCase extends AbstractJbpmTestCase
{
	static {
	   	System.setProperty( PROPERTY_MULE_TEST_TIMEOUT, "300");
	}
	
    protected String getConfigResources()
    {
        return "jbpm-functional-test.xml";
    }

    public void testVariables() throws Exception
    {
        BPMS bpms = connector.getBpms();
        MuleClient client = new MuleClient();
        try
        {
            Map props = new HashMap();
            props.put("foo", "bar");
            MuleMessage response = client.send("bpm://variables", "data", props);  
            String processId = (String) bpms.getId(response.getPayload());
            assertNotNull(processId);

        	response = client.request("vm://queueA", 3000);
        	assertNotNull(response);
            assertEquals("bar", response.getProperty("foo"));
            assertEquals(0.75, response.getProperty("fraction"));

            // Advance the process
            props = new HashMap();
            props.put("straw", "berry");
            props.put("time", new Date());
            response = client.send("bpm://variables/" + processId, "data", props);

            response = client.request("vm://queueB", 3000);
            assertNotNull(response);
            assertEquals("bar", response.getProperty("foo"));
            assertEquals(0.75, response.getProperty("fraction"));
            assertEquals("berry", response.getProperty("straw"));
            assertTrue(response.getProperty("time") instanceof Date);
        }
        finally
        {
            client.dispose();
        }
    }
}
