/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
