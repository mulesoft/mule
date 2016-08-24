/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.hamcrest.core.Is;
import org.junit.ClassRule;
import org.junit.Test;

public class TcpLengthFunctionalTestCase extends FunctionalTestCase {

  protected static String TEST_MESSAGE = "Test TCP Request";
  private int timeout = 60 * 1000 / 20;

  @ClassRule
  public static DynamicPort dynamicPort1 = new DynamicPort("port1");

  @ClassRule
  public static DynamicPort dynamicPort3 = new DynamicPort("port3");

  public TcpLengthFunctionalTestCase() {
    setDisposeContextPerClass(true);
  }

  @Override
  protected String getConfigFile() {
    return "tcp-length-functional-test-flow.xml";
  }

  @Test
  public void testSend() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage result = client.send("clientEndpoint", TEST_MESSAGE, null).getRight();
    assertEquals(TEST_MESSAGE + " Received", getPayloadAsString(result));
  }

  @Test
  public void testDispatchAndReply() throws Exception {
    MuleClient client = muleContext.getClient();
    client.dispatch("asyncClientEndpoint2", TEST_MESSAGE, null);
    // MULE-2754
    Thread.sleep(200);
    // expect failure - TCP simply can't work like this
    assertThat(client.request("asyncClientEndpoint2", timeout).getRight().isPresent(), is(false));
  }
}
