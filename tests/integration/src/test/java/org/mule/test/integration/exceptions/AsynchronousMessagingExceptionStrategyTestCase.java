/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class AsynchronousMessagingExceptionStrategyTestCase extends AbstractExceptionStrategyTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/asynch-messaging-exception-strategy.xml";
  }

  @Test
  public void testTransformerException() throws Exception {
    flowRunner("TransformerException").withPayload(getTestMuleMessage()).asynchronously().run();
    latch.await(LATCH_AWAIT_TIMEOUT, TimeUnit.MILLISECONDS);
    assertEquals(1, serviceExceptionCounter.get());
    assertEquals(0, systemExceptionCounter.get());
  }

  @Test
  public void testScriptComponentException() throws Exception {
    flowRunner("ScriptComponentException").withPayload(getTestMuleMessage()).asynchronously().run();
    latch.await(LATCH_AWAIT_TIMEOUT, TimeUnit.MILLISECONDS);
    assertEquals(1, serviceExceptionCounter.get());
    assertEquals(0, systemExceptionCounter.get());
  }

  @Test
  public void testCustomProcessorException() throws Exception {
    flowRunner("CustomProcessorException").withPayload(getTestMuleMessage()).asynchronously().run();
    latch.await(LATCH_AWAIT_TIMEOUT, TimeUnit.MILLISECONDS);
    assertEquals(1, serviceExceptionCounter.get());
    assertEquals(0, systemExceptionCounter.get());
  }
}


