/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.nested;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class BindingReturnTypeTestCase extends FunctionalTestCase
{
    private static final String PROCESSED = "Processed";
    private static final int MAGIC_NUMBER = 0xC0DE;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/nested/binding-returns-message.xml";
    }

    @Test
    public void testInvokeBinding() throws Exception
    {
        MuleClient client = muleContext.getClient();
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
