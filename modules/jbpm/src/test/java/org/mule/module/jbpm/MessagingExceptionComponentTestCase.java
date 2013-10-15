/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jbpm;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.exceptions.FunctionalTestException;
import org.mule.util.ExceptionUtils;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MessagingExceptionComponentTestCase extends FunctionalTestCase
{
    
    @Override
    protected String getConfigResources()
    {
        return "jbpm-component-functional-test.xml";
    }

    @Test
    public void testNoException() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.send("vm://exception", "testNoException", null);                                  

        // Both messages should have been sent.
        assertNotNull(client.request("vm://queueC", 1000));            
        assertNotNull(client.request("vm://queueD", 1000));            
    }

    @Test
    public void testExceptionInService() throws Exception
    {
        MuleClient client = muleContext.getClient();
        
        MuleMessage response = client.send("vm://exception", "testExceptionInService", null);                      
        assertNotNull("Should have thrown an exception", response.getExceptionPayload());
        assertTrue(ExceptionUtils.getRootCause(response.getExceptionPayload().getException()) instanceof FunctionalTestException);
        
        // The first message should have been sent, but not the second one.
        assertNotNull(client.request("vm://queueC", 1000));            
        assertNull(client.request("vm://queueD", 1000));            
    }

    @Test
    public void testExceptionInTransformer() throws Exception
    {
        MuleClient client = muleContext.getClient();
        
        MuleMessage response = client.send("vm://exception", "testExceptionInTransformer", null);                      
        assertNotNull("Should have thrown an exception", response.getExceptionPayload());
        assertTrue(ExceptionUtils.getRootCause(response.getExceptionPayload().getException()) instanceof TransformerException);
        
        // The first message should have been sent, but not the second one.
        assertNotNull(client.request("vm://queueC", 1000));            
        assertNull(client.request("vm://queueD", 1000));            
    }

}
