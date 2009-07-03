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
import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.MessageFactory;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class BindingExceptionOnInterfaceMethodTestCase extends FunctionalTestCase
{

    private static final String PREFIX = "Exception in service component: ";

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/nested/binding-exception-on-interface-method.xml";
    }
    
    public void testExceptionOnBinding() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage reply = client.send("vm://invoker.in", TEST_MESSAGE, null);
        assertNotNull(reply);
        String payload = reply.getPayloadAsString();
        assertTrue(payload.contains("MuleRuntimeException"));
        assertTrue(payload.contains(PREFIX));
    }

    public static class Component
    {
        private BindigInterface binding;
        
        public String invoke(String payload)
        {
            try
            {
                binding.process(payload, Integer.valueOf(0xC0DE));
            }
            catch (MuleRuntimeException muleException)
            {
                return PREFIX + muleException.toString();
            }
            
            return "ERROR, should not have come here";
        }

        public BindigInterface getBinding()
        {
            return binding;
        }

        public void setBinding(BindigInterface binding)
        {
            this.binding = binding;
        }
    }
    
    public static class ExceptionThrowingService
    {
        public String process(String s, Integer v)
        {
            throw new MuleRuntimeException(MessageFactory.createStaticMessage("Boom"));
        }
    }
    
    public interface BindigInterface
    {
        String process(String s, Integer v) throws MuleRuntimeException;
    }

}


