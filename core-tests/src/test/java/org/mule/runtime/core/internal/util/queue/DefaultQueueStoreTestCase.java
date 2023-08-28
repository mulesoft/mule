/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util.queue;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.util.queue.DefaultQueueConfiguration;
import org.mule.runtime.core.internal.util.queue.DefaultQueueStore;
import org.mule.runtime.core.internal.util.queue.QueueStore;
import org.mule.tck.core.util.queue.QueueStoreTestCase;

public class DefaultQueueStoreTestCase extends QueueStoreTestCase {

  @Override
  protected QueueStore createQueueInfoDelegate(int capacity, MuleContext mockMuleContext) {
    return new DefaultQueueStore("testQueue", mockMuleContext, new DefaultQueueConfiguration(capacity, false));
  }

}
