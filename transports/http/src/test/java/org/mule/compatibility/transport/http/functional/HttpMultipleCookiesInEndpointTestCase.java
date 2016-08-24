/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class HttpMultipleCookiesInEndpointTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "http-multiple-cookies-on-endpoint-test-flow.xml";
  }

  @Test
  public void testThatThe2CookiesAreSentAndReceivedByTheComponent() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage response = client.send("vm://in", "HELLO", null).getRight();
    assertNotNull(response);
    assertNotNull(response.getPayload());
    assertEquals("Both Cookies Found!", getPayloadAsString(response));
  }
}
