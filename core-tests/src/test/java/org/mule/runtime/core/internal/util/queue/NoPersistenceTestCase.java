/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
