/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.functional;


import static org.junit.Assert.assertEquals;

import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class HttpOutboundAttachmentFunctionalTestCase extends CompatibilityFunctionalTestCase {

  @Rule
  public DynamicPort httpPort = new DynamicPort("port");

  @Override
  protected String getConfigFile() {
    return "http-outbound-attachments-config.xml";
  }

  @Test
  public void sendsStringAttachmentCorrectly() throws Exception {
    sendMessageAndAssertResponse("vm://inString");
  }

  @Test
  public void sendsByteArrayAttachmentCorrectly() throws Exception {
    sendMessageAndAssertResponse("vm://inByteArray");
  }


  private void sendMessageAndAssertResponse(String endpoint) throws Exception {
    MuleClient client = muleContext.getClient();
    InternalMessage answer = client.send(endpoint, InternalMessage.builder().payload(TEST_MESSAGE).build()).getRight();
    assertEquals(TEST_MESSAGE, getPayloadAsString(answer));
  }
}
