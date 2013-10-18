/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.client;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class MuleClientInThreadTestCase extends AbstractServiceAndFlowTestCase
{
    int numMessages = 100000;

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/client/client-in-thread-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/client/client-in-thread-flow.xml"}
        });
    }

    public MuleClientInThreadTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testException() throws Exception
    {
        Thread tester1 = new Tester();
        tester1.start();
    }

    class Tester extends Thread
    {
        @Override
        public void run()
        {
            try
            {
                MuleClient client = new MuleClient(muleContext);

                for (int i = 0; i < numMessages; ++i)
                {
                    client.dispatch("vm://in", "test", null);
                }

                MuleMessage msg;
                for (int i = 0; i < numMessages; ++i)
                {
                    msg = client.request("vm://out", 5000);
                    assertNotNull(msg);
                }
            }
            catch (Exception e)
            {
                fail(e.getMessage());
            }
        }
    }
}
