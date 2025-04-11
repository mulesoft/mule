/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import static org.mule.tck.junit4.matcher.Eventually.eventually;
import static org.mule.tck.util.CollectableReference.collectedByGc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.util.queue.DefaultQueueConfiguration;
import org.mule.runtime.core.api.util.queue.Queue;
import org.mule.runtime.core.api.util.queue.QueueConfiguration;
import org.mule.runtime.core.api.util.queue.QueueSession;
import org.mule.runtime.core.internal.serialization.JavaObjectSerializer;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.util.CollectableReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TransactionalQueueManagerTestCase extends AbstractMuleTestCase {

  private static final String TEST_QUEUE_NAME = "queue1";

  private TransactionalQueueManager queueManager;

  @Before
  public void setUp() throws MuleException {
    queueManager = new TransactionalQueueManager();
    queueManager.setMuleConfiguration(new DefaultMuleConfiguration());
    queueManager.setObjectSerializer(new JavaObjectSerializer(this.getClass().getClassLoader()));
    queueManager.initialise();
    queueManager.start();
  }

  @After
  public void teardown() throws MuleException {
    queueManager.stop();
    queueManager.dispose();
  }

  @Test
  public void allowChangingConfigurationOnDisposedQueue() throws Exception {
    queueManager.setQueueConfiguration(TEST_QUEUE_NAME, new DefaultQueueConfiguration(0, true));
    QueueSession queueSession = queueManager.getQueueSession();
    Queue queue = queueSession.getQueue(TEST_QUEUE_NAME);
    queue.dispose();
    queueManager.setQueueConfiguration(TEST_QUEUE_NAME, new DefaultQueueConfiguration(0, false));
  }

  @Test
  public void clearRecoveryQueuesAfterRecovery() throws Exception {
    createDanglingTx();

    QueueSession queueSession = queueManager.getQueueSession();
    queueSession.getQueue(TEST_QUEUE_NAME).dispose();

    queueManager.setQueueConfiguration(TEST_QUEUE_NAME, new DefaultQueueConfiguration());
    queueManager.start();
  }

  @Test
  public void doNotCreateTwiceTheSameRecoveryQueue() {
    final RecoverableQueueStore recoveryQueue = queueManager.getRecoveryQueue(TEST_QUEUE_NAME);
    assertThat(recoveryQueue, is(queueManager.getRecoveryQueue(TEST_QUEUE_NAME)));
  }

  @Test
  public void doNotLeakQueueConfigurationAfterQueueDispose() {
    CollectableReference<QueueConfiguration> collectableReference = new CollectableReference<>(new DefaultQueueConfiguration());

    QueueStore queueStore = queueManager.getQueue(TEST_QUEUE_NAME);
    queueManager.setQueueConfiguration(TEST_QUEUE_NAME, collectableReference.get());
    queueManager.disposeQueueStore(queueStore);

    assertThat(collectableReference, is(eventually(collectedByGc())));
  }

  private void createDanglingTx() throws InterruptedException, MuleException {
    queueManager.setDefaultQueueConfiguration(new DefaultQueueConfiguration(0, true));
    QueueSession queueSession = queueManager.getQueueSession();
    queueSession.getQueue(TEST_QUEUE_NAME).put("value");
    queueSession.begin();
    queueSession.getQueue(TEST_QUEUE_NAME).poll(10);
    queueManager.stop();
  }
}
