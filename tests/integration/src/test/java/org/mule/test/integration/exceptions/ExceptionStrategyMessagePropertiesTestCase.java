/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class ExceptionStrategyMessagePropertiesTestCase extends FunctionalTestCase
{
    private final int numMessages = 100;
    
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/exceptions/exception-strategy-message-properties.xml";
    }

    @Test
    public void testException() throws Exception
    {
        Thread tester1 = new Tester();
        Thread tester2 = new Tester();
        tester1.start();
        tester2.start();
        
        MuleClient client = new MuleClient(muleContext);
        MuleMessage msg;
        for (int i = 0; i < numMessages; ++i)
        {
            msg = client.request("vm://error", 5000);
            assertNotNull(msg);
            assertEquals("bar", msg.getInboundProperty("foo"));
        }        
    }
    
    class Tester extends Thread
    {
        @Override
        public void run()
        {
            try
            {
                MuleClient client = new MuleClient(muleContext);
                
                Map props = new HashMap();
                props.put("foo", "bar");
                for (int i = 0; i < numMessages; ++i)
                {
                    client.dispatch("vm://in", "test", props);
                }    
            }
            catch (Exception e)
            {
                fail(e.getMessage());
            }
        }        
    };
}


