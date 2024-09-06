/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import org.mule.runtime.api.serialization.SerializationProtocol;
import org.mule.runtime.core.api.util.queue.DefaultQueueConfiguration;

public class DefaultQueueStoreTestCase extends QueueStoreTestCase {

  @Override
  protected QueueStore createQueueInfoDelegate(int capacity, String workingDirectory, SerializationProtocol serializer) {
    return new DefaultQueueStore("testQueue", workingDirectory, serializer, new DefaultQueueConfiguration(capacity, false));
  }

}
