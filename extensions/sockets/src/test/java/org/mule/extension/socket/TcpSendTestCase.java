/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket;

import org.mule.functional.junit4.runners.RunnerDelegateTo;

import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class TcpSendTestCase extends ParameterizedProtocolTestCase {


  @Override
  protected String getConfigFile() {
    return "tcp-send-config.xml";
  }

  @Test
  public void sendPojo() throws Exception {
    flowRunner("tcp-send").withPayload(testPojo).run();
    assertPojo(receiveConnection(), testPojo);
  }

  @Test
  public void sendString() throws Exception {

    flowRunner("tcp-send").withPayload(TEST_STRING).run();

    assertEvent(receiveConnection(), TEST_STRING);
  }

  @Test
  public void sendByteArray() throws Exception {
    flowRunner("tcp-send").withPayload(testByteArray).run();

    assertByteArray(receiveConnection(), testByteArray);
  }
}
