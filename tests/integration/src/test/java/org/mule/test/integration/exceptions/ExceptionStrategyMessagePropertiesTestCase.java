/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class ExceptionStrategyMessagePropertiesTestCase extends FunctionalTestCase
{
    private final int numMessages = 100;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/exceptions/exception-strategy-message-properties-flow.xml";
    }

    @Test
    public void testException() throws Exception
    {
        Thread tester1 = new Tester();
        Thread tester2 = new Tester();
        tester1.start();
        tester2.start();

        MuleClient client = muleContext.getClient();
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
                MuleClient client = muleContext.getClient();

                Map<String, Object> props = new HashMap<String, Object>();
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
    }
}
