/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.store;

import org.mule.runtime.dsl.api.component.AbstractAnnotatedObjectFactory;

/**
 * Factory used in mule 4 mechanism to create the QueueStore, depending on the profile (in memory, or persistence)
 */
public class QueueStoreObjectFactory extends AbstractAnnotatedObjectFactory<QueueStore> {

  private QueueStore store;

  public QueueStoreObjectFactory(QueueStore queueStore) {
    store = queueStore;
  }

  @Override
  public QueueStore doGetObject() throws Exception {
    return store;
  }
}
