/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.quartz;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

public class QuartzStatefulEventGeneratorTestCase extends AbstractQuartzStatefulTestCase
{
    private static final List<String> messages = new LinkedList<String>();
    private static final CountDownLatch latch = new CountDownLatch(1);

    @Override
    protected String getConfigFile()
    {
        return "quartz-stateful-event-generator-config.xml";
    }

    @Test
    public void testIssue() throws Exception
    {
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

