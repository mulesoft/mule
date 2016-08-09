/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket;

import org.junit.Test;

public class UdpSendTestCase extends SocketExtensionTestCase {

  @Override
  protected String getConfigFile() {
    return "udp-send-config.xml";
  }

  @Test
  public void sendPojo() throws Exception {
    sendPojo("udp-send");
  }

  @Test
  public void sendString() throws Exception {
    sendString("udp-send");
  }

  @Test
  public void sendByteArray() throws Exception {
    sendByteArray("udp-send");
  }

  @Test
  public void multipleSendString() throws Exception {
    for (int i = 0; i < 5; i++) {
      flowRunner("udp-send").withPayload(TEST_STRING).run();
    }

    for (int i = 0; i < 5; i++) {
      assertEvent(receiveConnection(), TEST_STRING);
    }
  }
}
