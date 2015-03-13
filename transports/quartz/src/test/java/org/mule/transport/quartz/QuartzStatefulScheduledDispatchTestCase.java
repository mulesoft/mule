/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.quartz;

import org.mule.api.client.LocalMuleClient;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

public class QuartzStatefulScheduledDispatchTestCase extends AbstractQuartzStatefulTestCase
{
    public static final String VM_TEST_INPUT = "vm://testInput";
    private static final List<String> messages = new LinkedList<String>();
    private static final CountDownLatch latch = new CountDownLatch(1);

    @Override
    protected String getConfigFile()
    {
        return "quartz-stateful-scheduled-dispatch-config.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        messages.clear();
    }

    @Test
    public void testIssue() throws Exception
    {
        messages.clear();
        LocalMuleClient client = muleContext.getClient();
        client.dispatch(VM_TEST_INPUT, TEST_MESSAGE, null);

        assertOnlyOneThreadWaiting(messages, latch);
    }

    public static class BlockingComponent
    {

        public String process(String payload) throws InterruptedException
        {
            synchronized (messages)
            {
                messages.add(payload);
            }
            latch.await();

            return payload;
        }
    }

}
