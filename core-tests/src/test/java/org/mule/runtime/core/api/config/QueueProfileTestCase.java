/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config;

import static org.mockito.Mockito.mock;
import static org.mule.runtime.core.api.config.QueueProfile.newInstancePersistingToDefaultMemoryQueueStore;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.core.api.config.QueueProfile.newInstanceWithPersistentQueueStore;
import org.junit.Test;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.util.queue.QueueConfiguration;
import org.mule.runtime.core.api.util.queue.QueueManager;

public class QueueProfileTestCase {

  @Test
  public void nonPersistentQueueProfile() throws InitialisationException {
    QueueProfile queueProfile = newInstancePersistingToDefaultMemoryQueueStore();
    assertThat(queueProfile.getMaxOutstandingMessages(), is(0));
    QueueConfiguration config = queueProfile.configureQueue("cmp", mock(QueueManager.class));
    assertThat(config.isPersistent(), is(false));
    assertThat(config.getCapacity(), is(0));
  }

  @Test
  public void persistentQueueProfile() throws InitialisationException {
    QueueProfile queueProfile = newInstanceWithPersistentQueueStore();
    assertThat(queueProfile.getMaxOutstandingMessages(), is(0));
    QueueConfiguration config = queueProfile.configureQueue("cmp", mock(QueueManager.class));
    assertThat(config.isPersistent(), is(true));
    assertThat(config.getCapacity(), is(0));
  }

  @Test
  public void customQueueProfile() throws InitialisationException {
    QueueProfile queueProfile = new QueueProfile(50, true);
    assertThat(queueProfile.getMaxOutstandingMessages(), is(50));
    queueProfile.setMaxOutstandingMessages(100);
    assertThat(queueProfile.getMaxOutstandingMessages(), is(100));
    QueueConfiguration config = queueProfile.configureQueue("cmp", mock(QueueManager.class));
    assertThat(config.isPersistent(), is(true));
    assertThat(config.getCapacity(), is(100));
  }

}
