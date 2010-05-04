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

import org.mule.api.transformer.TransformerException;
import org.mule.module.client.MuleClient;
import org.mule.tck.exceptions.FunctionalTestException;

public class MessagingExceptionTestCase extends AbstractJbpmTestCase
{
	static {
	   	System.setProperty( PROPERTY_MULE_TEST_TIMEOUT, "300");
	}
	
    protected String getConfigResources()
    {
        return "jbpm-functional-test.xml";
    }

    public void testNoException() throws Exception
    {
        MuleClient client = new MuleClient();
        client.send("bpm://exception", "testNoException", null);                                  

        // Both messages should have been sent.
        assertNotNull(client.request("vm://queueC", 1000));            
        assertNotNull(client.request("vm://queueD", 1000));            
    }

    public void testExceptionInService() throws Exception
    {
        MuleClient client = new MuleClient();
        try
        {
            client.send("bpm://exception", "testExceptionInService", null);                      
            fail("Should have thrown an exception");
        }
        catch (Exception e)
        {
            assertTrue(e.getCause().getCause().getCause() instanceof FunctionalTestException);
        }
        
        // The first message should have been sent, but not the second one.
        assertNotNull(client.request("vm://queueC", 1000));            
        assertNull(client.request("vm://queueD", 1000));            
    }

    public void testExceptionInTransformer() throws Exception
    {
        MuleClient client = new MuleClient();
        try
        {
            client.send("bpm://exception", "testExceptionInTransformer", null);                      
            fail("Should have thrown an exception");
        }
        catch (Exception e)
        {
            assertTrue(e.getCause().getCause().getCause() instanceof TransformerException);
        }
        
        // The first message should have been sent, but not the second one.
        assertNotNull(client.request("vm://queueC", 1000));            
        assertNull(client.request("vm://queueD", 1000));            
    }
}
