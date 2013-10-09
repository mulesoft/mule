/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    protected String getConfigResources()
    {
        return "quartz-stateful-scheduled-dispatch-config.xml";
    }

    @Test
    public void testIssue() throws Exception
    {
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
