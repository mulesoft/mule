/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import org.junit.Test;

/**
 * This test re-written to use entry point resolvers. As a consequence, some tests, which verified implementation details rather
 * than functionality, were dropped.
 */
public class NoArgsCallComponentTestCase extends AbstractIntegrationTestCase {

  public static final String INPUT_DC_FLOW_NAME = "WORMS";
  public static final String OUTPUT_DC_QUEUE_NAME = "test://out";
  public static final String INPUT_DI_FLOW_NAME = "TIRANA";
  public static final String OUTPUT_DI_QUEUE_NAME = "test://outWithInjected";

  public static final String DEFAULT_OUTPUT_MESSAGE = "Just an apple.";

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/components/no-args-call-component-functional-test-flow.xml";
  }

  @Test
  public void testDelegateClass() throws Exception {
    MuleClient client = muleContext.getClient();
    flowRunner(INPUT_DC_FLOW_NAME).withPayload(TEST_PAYLOAD).asynchronously().run();
    MuleMessage message = client.request(OUTPUT_DC_QUEUE_NAME, RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(message);
    assertEquals(message.getPayload(), DEFAULT_OUTPUT_MESSAGE);
  }

  @Test
  public void testWithInjectedDelegate() throws Exception {
    MuleClient client = muleContext.getClient();
    flowRunner(INPUT_DI_FLOW_NAME).withPayload(TEST_PAYLOAD).asynchronously().run();
    MuleMessage reply = client.request(OUTPUT_DI_QUEUE_NAME, RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(reply);
    // same as original input
    assertEquals(TEST_PAYLOAD, reply.getPayload());
  }
}
