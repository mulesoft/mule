/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp.protocols;

import static org.junit.Assert.assertEquals;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class MuleMessageLengthTestCase extends FunctionalTestCase {

  protected static String TEST_MESSAGE = "Test TCP Request";

  @Rule
  public DynamicPort port1 = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "tcp-mplength-test-flow.xml";
  }

  @Test
  public void testSend() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage result = client.send("clientEndpoint", TEST_MESSAGE, null).getRight();
    assertEquals(TEST_MESSAGE + " Received", getPayloadAsString(result));
  }
}
