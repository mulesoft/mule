/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UdpSendAndReceiveStringTestCase extends SocketExtensionTestCase {

  @Override
  protected String getConfigFile() {
    return "udp-send-and-receive-string-config.xml";
  }

  @Test
  public void sendStringAndReceiveModifiedString() throws Exception {
    String response =
        (String) flowRunner("udp-send-and-receive").withPayload(TEST_STRING).run().getMessage().getPayload().getValue();
    assertEquals(response, RESPONSE_TEST_STRING);
  }
}
