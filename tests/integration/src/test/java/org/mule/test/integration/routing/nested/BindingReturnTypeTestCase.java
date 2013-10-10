/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.routing.nested;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BindingReturnTypeTestCase extends FunctionalTestCase
{

    private static final String PROCESSED = "Processed";
    private static final int MAGIC_NUMBER = 0xC0DE;

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/nested/binding-returns-message.xml";
    }

    @Test
    public void testInvokeBinding() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("vm://invoker.in", TEST_MESSAGE, null);
        assertNotNull(response);
        assertNull(response.getExceptionPayload());
        //TODO MULE-4990 this should really be in the inbound scope 
        assertTrue(response.getInboundProperty(PROCESSED, false));
        //assertTrue(response.getOutboundProperty(PROCESSED, false));
        String expected = "Hello " + TEST_MESSAGE + " " + MAGIC_NUMBER;
        assertEquals(expected, response.getPayload());
    }

    public static class Component
    {
        private BindingInterface binding;

        public Object invoke(String s)
        {
            MuleMessage result = binding.process(s, new Integer(MAGIC_NUMBER));
            result.setOutboundProperty(PROCESSED, Boolean.TRUE);
            return result;
        }
        
        public void setBindingInterface(BindingInterface hello)
        {
            this.binding = hello;
        }

        public BindingInterface getBindingInterface()
        {
            return binding;
        }
    }
    
    public interface BindingInterface
    {
        MuleMessage process(String s, Integer v);
    }
    
}


