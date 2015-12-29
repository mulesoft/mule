/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.functional.junit4.FunctionalTestCase;

import org.junit.Test;

public class MuleClientInThreadTestCase extends FunctionalTestCase
{
    int numMessages = 100;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/client/client-in-thread-flow.xml";
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
                for (int i = 0; i < numMessages; ++i)
                {
                    runFlowAsync("testFlow", TEST_MESSAGE);
                }

                MuleClient client = new MuleClient(muleContext);
                MuleMessage msg;
                for (int i = 0; i < numMessages; ++i)
                {
                    msg = client.request("test://out", RECEIVE_TIMEOUT);
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
