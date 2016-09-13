/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.messaging.meps;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.mule.functional.junit4.FlowRunner;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class InOnlyOutOnlyTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/messaging/meps/pattern_In-Only_Out-Only-flow.xml";
  }

  @Test
  public void testExchange() throws Exception {
    MuleClient client = muleContext.getClient();

    FlowRunner baseRunner = flowRunner("In-Only_Out-Only-Service").withPayload("some data").asynchronously();
    baseRunner.run();
    baseRunner.reset();
    baseRunner.withInboundProperty("foo", "bar").run();

    InternalMessage result = client.request("test://received", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(result);
    assertThat(getPayloadAsString(result), is("foo header received"));

    result = client.request("test://notReceived", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(result);
    assertThat(getPayloadAsString(result), is("foo header not received"));
  }
}
