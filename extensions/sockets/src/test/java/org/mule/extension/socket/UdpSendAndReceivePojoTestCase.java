/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket;

import static org.junit.Assert.assertEquals;
import static org.mule.extension.socket.TcpSendAndReceivePojoTestCase.RESPONSE_AGE;
import static org.mule.extension.socket.TcpSendAndReceivePojoTestCase.RESPONSE_NAME;

import org.mule.runtime.api.message.MuleMessage;

import org.junit.Test;

public class UdpSendAndReceivePojoTestCase extends SocketExtensionTestCase {

  @Override
  protected String getConfigFile() {
    return "udp-send-and-receive-pojo-config.xml";
  }

  @Test
  public void sendAndReceivePojo() throws Exception {
    MuleMessage message = flowRunner("udp-send-and-receive").withPayload(testPojo).run().getMessage();

    TestPojo pojo = (TestPojo) deserializeMessage(message);
    assertEquals(pojo.getAge(), RESPONSE_AGE);
    assertEquals(pojo.getName(), RESPONSE_NAME);
  }
}
