/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_QUEUE_MANAGER;
import static org.mule.tck.junit4.matcher.Eventually.eventually;
import static org.mule.tck.util.CollectableReference.collectedByGc;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.util.queue.DefaultQueueConfiguration;
import org.mule.runtime.core.api.util.queue.Queue;
import org.mule.runtime.core.api.util.queue.QueueConfiguration;
import org.mule.runtime.core.api.util.queue.QueueManager;
import org.mule.runtime.core.api.util.queue.QueueSession;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.util.CollectableReference;

import org.junit.Test;

public class TransactionalQueueManagerTestCase extends AbstractMuleContextTestCase {

  private static final String TEST_QUEUE_NAME = "queue1";

  @Test
  public void allowChangingConfigurationOnDisposedQueue() throws Exception {
    QueueManager queueManager = ((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(OBJECT_QUEUE_MANAGER);
    queueManager.setQueueConfiguration(TEST_QUEUE_NAME, new DefaultQueueConfiguration(0, true));
    QueueSession queueSession = queueManager.getQueueSession();
    Queue queue = queueSession.getQueue(TEST_QUEUE_NAME);
    queue.dispose();
    queueManager.setQueueConfiguration(TEST_QUEUE_NAME, new DefaultQueueConfiguration(0, false));
  }

  @Test
  public void clearRecoveryQueuesAfterRecovery() throws Exception {
    createDanglingTx();

    QueueManager queueManager = ((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(OBJECT_QUEUE_MANAGER);
    QueueSession queueSession = queueManager.getQueueSession();
    queueSession.getQueue(TEST_QUEUE_NAME).dispose();

    queueManager.setQueueConfiguration(TEST_QUEUE_NAME, new DefaultQueueConfiguration());
    queueManager.start();
  }

  @Test
  public void doNotCreateTwiceTheSameRecoveryQueue() {
    TransactionalQueueManager queueManager =
        ((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(OBJECT_QUEUE_MANAGER);
    final RecoverableQueueStore recoveryQueue = queueManager.getRecoveryQueue(TEST_QUEUE_NAME);
    assertThat(recoveryQueue, is(queueManager.getRecoveryQueue(TEST_QUEUE_NAME)));
  }

  @Test
  public void doNotLeakQueueConfigurationAfterQueueDispose() {
    TransactionalQueueManager queueManager =
        ((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(OBJECT_QUEUE_MANAGER);
    CollectableReference<QueueConfiguration> collectableReference = new CollectableReference<>(new DefaultQueueConfiguration());

    QueueStore queueStore = queueManager.getQueue(TEST_QUEUE_NAME);
    queueManager.setQueueConfiguration(TEST_QUEUE_NAME, collectableReference.get());
    queueManager.disposeQueueStore(queueStore);

    assertThat(collectableReference, is(eventually(collectedByGc())));
  }

  @Override
  protected boolean isStartContext() {
    return true;
  }

  private void createDanglingTx() throws InterruptedException, MuleException {
    QueueManager queueManager = ((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(OBJECT_QUEUE_MANAGER);
    queueManager.setDefaultQueueConfiguration(new DefaultQueueConfiguration(0, true));
    QueueSession queueSession = queueManager.getQueueSession();
    queueSession.getQueue(TEST_QUEUE_NAME).put("value");
    queueSession.begin();
    queueSession.getQueue(TEST_QUEUE_NAME).poll(10);
    queueManager.stop();
  }
}
