/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.routing.nested;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class BindingReturnTypeTestCase extends FunctionalTestCase
{

    private static final String PROCESSED = "Processed";
    private static final int MAGIC_NUMBER = 0xC0DE;

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/nested/binding-returns-message.xml";
    }

    public void testInvokeBinding() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage response = client.send("vm://invoker.in", TEST_MESSAGE, null);
        assertNotNull(response);
        assertNull(response.getExceptionPayload());
        assertTrue(response.getBooleanProperty(PROCESSED, false));
        String expected = "Hello " + TEST_MESSAGE + " " + MAGIC_NUMBER;
        assertEquals(expected, response.getPayload());
    }

    public static class Component
    {
        private BindigInterface binding;

        public Object invoke(String s)
        {
            MuleMessage result = binding.process(s, new Integer(MAGIC_NUMBER));
            result.setProperty(PROCESSED, Boolean.TRUE);
            return result;
        }
        
        public void setBindingInterface(BindigInterface hello)
        {
            this.binding = hello;
        }

        public BindigInterface getBindingInterface()
        {
            return binding;
        }
    }
    
    public interface BindigInterface
    {
        MuleMessage process(String s, Integer v);
    }
    
}


