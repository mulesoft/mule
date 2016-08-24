/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.functional;

import static org.junit.Assert.assertEquals;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class HttpEndpointConstructTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort dynamicPort1 = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "http-endpoint-construct-conf.xml";
  }

  @Test
  public void testHttpEndpointConstruct() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage response = client.send("http://localhost:" + dynamicPort1.getNumber() + "/testA", TEST_MESSAGE, null).getRight();
    assertEquals(TEST_MESSAGE, getPayloadAsString(response));
  }
}
