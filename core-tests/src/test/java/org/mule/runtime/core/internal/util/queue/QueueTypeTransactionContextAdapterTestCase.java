/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import static org.mockito.Mockito.verify;

import org.mule.runtime.core.internal.util.queue.QueueStore;
import org.mule.runtime.core.internal.util.queue.QueueTransactionContext;
import org.mule.runtime.core.internal.util.queue.QueueTransactionContextFactory;
import org.mule.runtime.core.internal.util.queue.QueueTypeTransactionContextAdapter;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.Serializable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class QueueTypeTransactionContextAdapterTestCase extends AbstractMuleTestCase {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private QueueTransactionContextFactory<QueueTransactionContext> mockQueueTransactionContextFactory;
  @Mock
  private QueueStore mockQueueStore;
  @Mock
  private Serializable mockValue;

  @Test
  public void createPersistentContextWhenQueueIsPersistent() throws InterruptedException {
    QueueTransactionContext queueTransactionContext =
        new QueueTypeTransactionContextAdapter<QueueTransactionContext>(mockQueueTransactionContextFactory);
    Mockito.when(mockQueueStore.isPersistent()).thenReturn(true);
    queueTransactionContext.offer(mockQueueStore, mockValue, 10);
    verify(mockQueueTransactionContextFactory.createPersistentTransactionContext());
  }

  @Test
  public void createTransientContextWhenQueueIsPersistent() throws InterruptedException {
    QueueTransactionContext queueTransactionContext =
        new QueueTypeTransactionContextAdapter<QueueTransactionContext>(mockQueueTransactionContextFactory);
    Mockito.when(mockQueueStore.isPersistent()).thenReturn(false);
    queueTransactionContext.offer(mockQueueStore, mockValue, 10);
    verify(mockQueueTransactionContextFactory.createTransientTransactionContext());
  }
}
