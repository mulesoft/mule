/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.queue;

import org.mule.api.MuleException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class TransactionalQueueManagerTestCase extends AbstractMuleContextTestCase
{

    public static final String TEST_QUEUE_NAME = "queue1";

    @Test
    public void allowChangingConfigurationOnDisposedQueue() throws Exception
    {
        muleContext.start();
        QueueManager queueManager = muleContext.getQueueManager();
        queueManager.setQueueConfiguration(TEST_QUEUE_NAME, new DefaultQueueConfiguration(0, true));
        QueueSession queueSession = queueManager.getQueueSession();
        Queue queue = queueSession.getQueue(TEST_QUEUE_NAME);
        queue.dispose();
        queueManager.setQueueConfiguration(TEST_QUEUE_NAME, new DefaultQueueConfiguration(0, false));
    }

    @Test
    public void clearRecoveryQueuesAfterRecovery() throws Exception
    {
        muleContext.start();

        createDanglingTx();

        QueueManager queueManager = muleContext.getQueueManager();
        QueueSession queueSession = queueManager.getQueueSession();
        queueSession.getQueue(TEST_QUEUE_NAME).dispose();

        queueManager.setQueueConfiguration(TEST_QUEUE_NAME, new DefaultQueueConfiguration());
        queueManager.start();
    }

    private void createDanglingTx() throws InterruptedException, MuleException
    {
        QueueManager queueManager = muleContext.getQueueManager();
        queueManager.setDefaultQueueConfiguration(new DefaultQueueConfiguration(0, true));
        QueueSession queueSession = queueManager.getQueueSession();
        queueSession.getQueue(TEST_QUEUE_NAME).put("value");
        queueSession.begin();
        queueSession.getQueue(TEST_QUEUE_NAME).poll(10);
        queueManager.stop();
    }
}
