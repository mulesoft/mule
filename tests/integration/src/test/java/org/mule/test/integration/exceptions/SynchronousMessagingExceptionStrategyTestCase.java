/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import org.junit.Test;

public class SynchronousMessagingExceptionStrategyTestCase extends AbstractExceptionStrategyTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/synch-messaging-exception-strategy.xml";
  }

  @Test
  public void testTransformer() throws Exception {
    flowRunner("Transformer").withPayload(TEST_PAYLOAD).dispatch();
    exceptionListener.waitUntilAllNotificationsAreReceived();
    systemExceptionListener.assertNotInvoked();
  }

  @Test
  public void testComponent() throws Exception {
    flowRunner("Component").withPayload(TEST_PAYLOAD).dispatch();
    exceptionListener.waitUntilAllNotificationsAreReceived();
    systemExceptionListener.assertNotInvoked();
  }

  @Test
  public void testProcessorInboundRouter() throws Exception {
    flowRunner("ProcessorInboundRouter").withPayload(TEST_PAYLOAD).dispatch();
    exceptionListener.waitUntilAllNotificationsAreReceived();
    systemExceptionListener.assertNotInvoked();
  }

  @Test
  public void testProcessorOutboundRouter() throws Exception {
    flowRunner("ProcessorOutboundRouter").withPayload(TEST_PAYLOAD).dispatch();
    exceptionListener.waitUntilAllNotificationsAreReceived();
    systemExceptionListener.assertNotInvoked();
  }
}
