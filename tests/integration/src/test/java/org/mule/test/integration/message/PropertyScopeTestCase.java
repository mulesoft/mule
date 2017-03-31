/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.message;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.test.IntegrationTestCaseRunnerConfig;

import org.junit.Test;

public class PropertyScopeTestCase extends AbstractPropertyScopeTestCase implements IntegrationTestCaseRunnerConfig {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/message/property-scope-flow.xml";
  }

  @Test
  public void testRequestResponseChain() throws Exception {
    Message result = flowRunner("s1").withPayload(TEST_PAYLOAD).withInboundProperty("foo", "fooValue").run().getMessage();

    assertThat(result.getPayload().getValue(), is("test bar"));
    assertThat(((InternalMessage) result).getOutboundProperty("foo4"), is("fooValue"));
  }

  @Test
  public void testOneWay() throws Exception {
    flowRunner("oneWay").withPayload(TEST_PAYLOAD).withInboundProperty("foo", "fooValue").run();

    MuleClient client = muleContext.getClient();
    Message result = client.request("test://queueOut", RECEIVE_TIMEOUT).getRight().get();

    assertThat(result.getPayload().getValue(), is("test bar"));
    assertThat(((InternalMessage) result).getOutboundProperty("foo2"), is("fooValue"));
  }

  @Test
  public void testRRToOneWay() throws Exception {
    flowRunner("rrToOneWay").withPayload(TEST_PAYLOAD).withInboundProperty("foo", "rrfooValue").run();

    MuleClient client = muleContext.getClient();
    Message result = client.request("test://rrQueueOut", RECEIVE_TIMEOUT).getRight().get();

    assertThat(result.getPayload().getValue(), is("test baz"));
    assertThat(((InternalMessage) result).getOutboundProperty("foo2"), is("rrfooValue"));
  }
}
