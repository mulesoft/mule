/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.ws.functional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class TimeoutFunctionalTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port");

  @Override
  protected String getConfigFile() {
    return "timeout-config.xml";
  }

  @Test
  public void flowAndSessionVarsAreNotRemovedAfterTimeout() throws Exception {
    final Latch serverLatch = new Latch();

    getFunctionalTestComponent("server").setEventCallback((context, component, muleContext) -> serverLatch.await());

    flowRunner("client").withPayload("<echo/>").run();
    serverLatch.release();

    MuleMessage message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT).getRight().get();

    assertThat(message.<String>getOutboundProperty("flowVar"), equalTo("testFlowVar"));
  }
}
