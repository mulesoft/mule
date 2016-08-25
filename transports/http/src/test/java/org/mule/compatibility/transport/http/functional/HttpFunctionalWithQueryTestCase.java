/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.functional;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

public class HttpFunctionalWithQueryTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "http-functional-test-with-query-flow.xml";
  }

  @Test
  public void testSend() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage result = client.send("clientEndpoint1", MuleMessage.builder().nullPayload().build()).getRight();
    assertEquals("boobar", getPayloadAsString(result));
  }

  @Test
  public void testSendWithParams() throws Exception {
    MuleClient client = muleContext.getClient();
    Map<String, Serializable> props = new HashMap<>();
    props.put("foo", "noo");
    props.put("far", "nar");
    MuleMessage result = client.send("clientEndpoint2", null, props).getRight();
    assertEquals("noonar", getPayloadAsString(result));
  }

  @Test
  public void testSendWithBadParams() throws Exception {
    MuleClient client = muleContext.getClient();
    Map<String, Serializable> props = new HashMap<>();
    props.put("hoo", "noo");
    props.put("har", "nar");

    try {
      client.send("clientEndpoint2", null, props);
      fail("Required values missing");
    } catch (Exception e) {
      assertThat(e.getCause(), instanceOf(ExpressionRuntimeException.class));
    }
  }
}
