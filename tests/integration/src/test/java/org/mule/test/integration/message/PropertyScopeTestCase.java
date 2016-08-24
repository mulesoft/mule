/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.message;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import org.junit.Test;

public class PropertyScopeTestCase extends AbstractPropertyScopeTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/message/property-scope-flow.xml";
  }

  @Test
  public void testRequestResponseChain() throws Exception {
    MuleMessage result = flowRunner("s1").withPayload(TEST_PAYLOAD).withInboundProperty("foo", "fooValue").run().getMessage();

    assertThat(result.getPayload(), is("test bar"));
    assertThat(result.getOutboundProperty("foo4"), is("fooValue"));
  }

  @Test
  public void testOneWay() throws Exception {
    flowRunner("oneWay").withPayload(TEST_PAYLOAD).withInboundProperty("foo", "fooValue").asynchronously().run();

    MuleClient client = muleContext.getClient();
    MuleMessage result = client.request("test://queueOut", RECEIVE_TIMEOUT).getRight().get();
    assertThat(result.getPayload(), is("test bar"));
    assertThat(result.getOutboundProperty("foo2"), is("fooValue"));
  }

  @Test
  public void testRRToOneWay() throws Exception {
    flowRunner("rrToOneWay").withPayload(TEST_PAYLOAD).withInboundProperty("foo", "rrfooValue").asynchronously().run();

    MuleClient client = muleContext.getClient();
    MuleMessage result = client.request("test://rrQueueOut", RECEIVE_TIMEOUT).getRight().get();
    assertThat(result.getPayload(), is("test baz"));
    assertThat(result.getOutboundProperty("foo2"), is("rrfooValue"));
  }
}
