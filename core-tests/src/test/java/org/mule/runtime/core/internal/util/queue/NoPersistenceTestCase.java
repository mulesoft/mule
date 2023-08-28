/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util.queue;

import org.mule.runtime.core.api.util.queue.DefaultQueueConfiguration;
import org.mule.tck.core.util.queue.AbstractTransactionQueueManagerTestCase;

public class NoPersistenceTestCase extends AbstractTransactionQueueManagerTestCase {

  @Override
  protected TransactionalQueueManager createQueueManager() throws Exception {
    TransactionalQueueManager mgr = new TransactionalQueueManager();
    mgr.setMuleContext(muleContext);
    mgr.initialise();
    mgr.setDefaultQueueConfiguration(new DefaultQueueConfiguration(0, false));
    return mgr;
  }

  @Override
  protected boolean isPersistent() {
    return false;
  }
}
