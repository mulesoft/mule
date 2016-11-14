/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp.integration;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.mule.compatibility.core.api.FutureMessageResult;
import org.mule.compatibility.module.client.MuleClient;
import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.concurrent.TimeoutException;

import org.junit.Rule;
import org.junit.Test;

public class SocketTimeoutTestCase extends CompatibilityFunctionalTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "tcp-outbound-timeout-config.xml";
  }

  @Test
  public void socketReadWriteResponseTimeout() throws Exception {
    final MuleClient client = new MuleClient(muleContext);
    FutureMessageResult result = client.sendAsync("vm://inboundTest1", "something", null);
    InternalMessage message = null;
    try {
      message = result.getResult(1000).getRight();
    } catch (TimeoutException e) {
      fail("Response timeout not honored.");
    }
    assertNotNull(message);
  }

  @Test
  public void socketConnectionResponseTimeout() throws Exception {
    final MuleClient client = new MuleClient(muleContext);
    FutureMessageResult result = client.sendAsync("vm://inboundTest2", "something", null);
    InternalMessage message = null;
    try {
      message = result.getResult(1000).getRight();
    } catch (TimeoutException e) {
      fail("Response timeout not honored.");
    }
    assertNotNull(message);
  }

}
