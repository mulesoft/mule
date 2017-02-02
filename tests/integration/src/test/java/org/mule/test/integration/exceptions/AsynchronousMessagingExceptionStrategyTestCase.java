/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import org.junit.Test;

public class AsynchronousMessagingExceptionStrategyTestCase extends AbstractExceptionStrategyTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/asynch-messaging-exception-strategy.xml";
  }

  @Test
  public void testTransformerException() throws Exception {
    flowRunner("TransformerException").withPayload(TEST_PAYLOAD).dispatch();
    exceptionListener.waitUntilAllNotificationsAreReceived();
    systemExceptionListener.assertNotInvoked();
  }

  @Test
  public void testScriptComponentException() throws Exception {
    flowRunner("ScriptComponentException").withPayload(TEST_PAYLOAD).dispatch();
    exceptionListener.waitUntilAllNotificationsAreReceived();
    systemExceptionListener.assertNotInvoked();
  }

  @Test
  public void testCustomProcessorException() throws Exception {
    flowRunner("CustomProcessorException").withPayload(TEST_PAYLOAD).dispatch();
    exceptionListener.waitUntilAllNotificationsAreReceived();
    systemExceptionListener.assertNotInvoked();
  }
}


