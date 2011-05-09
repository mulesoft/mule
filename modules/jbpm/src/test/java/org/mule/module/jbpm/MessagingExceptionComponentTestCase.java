/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.jbpm;

import org.mule.api.client.MuleClient;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.exceptions.FunctionalTestException;
import org.mule.util.ExceptionUtils;

public class MessagingExceptionComponentTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "jbpm-component-functional-test.xml";
    }

    public void testNoException() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.send("vm://exception", "testNoException", null);                                  

        // Both messages should have been sent.
        assertNotNull(client.request("vm://queueC", 1000));            
        assertNotNull(client.request("vm://queueD", 1000));            
    }

    public void testExceptionInService() throws Exception
    {
        MuleClient client = muleContext.getClient();        
        try
        {
            client.send("vm://exception", "testExceptionInService", null);                      
            fail("Should have thrown an exception");
        }
        catch (Exception e)
        {
            assertTrue(ExceptionUtils.getRootCause(e) instanceof FunctionalTestException);
        }
        
        // The first message should have been sent, but not the second one.
        assertNotNull(client.request("vm://queueC", 1000));            
        assertNull(client.request("vm://queueD", 1000));            
    }

    public void testExceptionInTransformer() throws Exception
    {
        MuleClient client = muleContext.getClient();
        
        try
        {
            client.send("vm://exception", "testExceptionInTransformer", null);
            fail("Should have thrown an exception");
        }
        catch (Exception e)
        {
            assertTrue(ExceptionUtils.getRootCause(e) instanceof TransformerException);
        }
        
        // The first message should have been sent, but not the second one.
        assertNotNull(client.request("vm://queueC", 1000));            
        assertNull(client.request("vm://queueD", 1000));            
    }
}
