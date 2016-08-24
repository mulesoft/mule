/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.ssl.issues;

import static org.junit.Assert.assertEquals;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import org.junit.Test;

public class MultipleConnectorsMule1765TestCase extends FunctionalTestCase {

  protected static String TEST_SSL_MESSAGE = "Test SSL Request";

  @Override
  protected String getConfigFile() {
    return "multiple-connectors-test-flow.xml";
  }

  @Test
  public void testSend() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage result = client.send("clientEndpoint", TEST_SSL_MESSAGE, null).getRight();
    assertEquals(TEST_SSL_MESSAGE + " Received", getPayloadAsString(result));
  }
}
