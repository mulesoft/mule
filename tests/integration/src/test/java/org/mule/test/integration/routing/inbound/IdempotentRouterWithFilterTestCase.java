/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.inbound;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class IdempotentRouterWithFilterTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/inbound/idempotent-router-with-filter-flow.xml";
  }

  /**
   * This test will pass a message containing a String to the Mule server and verifies that it gets received.
   * 
   * @throws Exception
   */
  @Test
  @SuppressWarnings("null")
  public void testWithValidData() throws Exception {
    flowRunner("IdempotentPlaceHolder").withPayload("Mule is the best!").run();
    MuleClient myClient = muleContext.getClient();
    Message response = myClient.request("test://ToTestCase", RECEIVE_TIMEOUT).getRight().get();

    assertNotNull(response);
    assertNotNull(response.getPayload().getValue());
    assertThat(response.getPayload().getValue(), is("Mule is the best!"));
  }

  /**
   * This test will pass a message containing an Object to the Mule server and verifies that it does not get received.
   * 
   * @throws Exception
   */
  @Test
  public void testWithInvalidData() throws Exception {
    flowRunner("IdempotentPlaceHolder").withPayload(new Object()).run();
    MuleClient myClient = muleContext.getClient();
    assertThat(myClient.request("test://ToTestCase", RECEIVE_TIMEOUT).getRight().isPresent(), is(false));
  }
}
