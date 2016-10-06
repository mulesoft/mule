/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.vm.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.util.queue.DefaultQueueConfiguration;
import org.mule.runtime.core.util.queue.TransactionalQueueManager;

import org.junit.Test;

public class PersistentRecoverMessageTestCase extends CompatibilityFunctionalTestCase {

  public static final String TEST_QUEUE_NAME = "flowOut";

  public PersistentRecoverMessageTestCase() {
    setStartContext(false);
  }

  @Override
  protected String getConfigFile() {
    return "vm/persistent-vmqueue-test.xml";
  }

  @Test
  public void testRecoverMessage() throws Exception {
    TransactionalQueueManager transactionalQueueManager = new TransactionalQueueManager();
    transactionalQueueManager.setMuleContext(muleContext);
    transactionalQueueManager.setQueueConfiguration(TEST_QUEUE_NAME, new DefaultQueueConfiguration(0, true));
    transactionalQueueManager.initialise();
    transactionalQueueManager.start();
    transactionalQueueManager.getQueueSession().getQueue(TEST_QUEUE_NAME).put(InternalMessage.of("echo"));

    transactionalQueueManager.stop();

    muleContext.start();

    MuleClient client = muleContext.getClient();
    InternalMessage result = client.request("vm://" + TEST_QUEUE_NAME, RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(result);
    assertEquals(getPayloadAsString(InternalMessage.of("echo")), result.getPayload().getValue());
  }
}


