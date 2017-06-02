/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.core.util.queue;

import static junit.framework.Assert.assertNotSame;
import static org.junit.Assert.assertEquals;

import org.mule.runtime.core.api.util.queue.Queue;
import org.mule.runtime.core.api.util.queue.QueueManager;
import org.mule.runtime.core.api.util.queue.QueueSession;

public class QueueTestComponent {

  public void testDisposal(QueueManager mgr, boolean transactional) throws Exception {
    final String queueName = "myQueue";
    mgr.start();
    QueueSession session = mgr.getQueueSession();

    if (transactional) {
      session.begin();
    }

    Queue queue = mgr.getQueueSession().getQueue(queueName);
    this.assertQueueDisposal(mgr, queue, transactional ? session : null, queueName);
  }

  private void assertQueueDisposal(QueueManager mgr, Queue queue, QueueSession session, String queueName) throws Exception {
    queue.put("some value");
    assertEquals(1, queue.size());

    if (session != null) {
      session.commit();
    }

    queue.dispose();

    Queue queue2 = mgr.getQueueSession().getQueue(queueName);
    assertNotSame(queue, queue2);
    assertEquals(0, queue2.size());
  }

}
