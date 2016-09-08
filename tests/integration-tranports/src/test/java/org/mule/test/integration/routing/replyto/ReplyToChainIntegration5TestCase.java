/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.replyto;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_REPLY_TO_PROPERTY;

import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.util.concurrent.Latch;

import org.junit.Test;

public class ReplyToChainIntegration5TestCase extends FunctionalTestCase {

  public static final String TEST_PAYLOAD = "test payload";
  public static final String EXPECTED_PAYLOAD = TEST_PAYLOAD + " modified";
  public static final int TIMEOUT = 5000;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/replyto/replyto-chain-integration-test-5-flow.xml";
  }

  @Test
  public void testReplyToIsHonoredInFlowUsingAsyncBlock() throws Exception {
    MuleClient client = muleContext.getClient();
    final Latch flowExecutedLatch = new Latch();
    FunctionalTestComponent ftc = getFunctionalTestComponent("replierService");
    ftc.setEventCallback((context, component, muleContext) -> flowExecutedLatch.release());
    MuleMessage muleMessage =
        MuleMessage.builder().payload(TEST_PAYLOAD).addOutboundProperty(MULE_REPLY_TO_PROPERTY, "jms://response").build();
    client.dispatch("jms://jmsIn1", muleMessage);
    flowExecutedLatch.await(TIMEOUT, MILLISECONDS);
    MuleMessage response = client.request("jms://response", TIMEOUT).getRight().get();
    assertThat(response, notNullValue());
    assertThat(getPayloadAsString(response), is(EXPECTED_PAYLOAD));
  }
}
