/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket;

import static org.junit.Assert.assertEquals;

import org.mule.runtime.api.message.Message;

import org.junit.Test;

public class TcpSendAndReceivePojoTestCase extends ParameterizedProtocolTestCase {

  public static final int RESPONSE_AGE = 7;
  public static final String RESPONSE_NAME = "Ronaldo";

  @Override
  protected String getConfigFile() {
    return "tcp-send-and-receive-pojo-config.xml";
  }

  @Test
  public void sendAndReceivePojo() throws Exception {
    Message message = flowRunner("tcp-send-and-receive").withPayload(testPojo).run().getMessage();

    TestPojo pojo = (TestPojo) deserializeMessage(message);
    assertEquals(pojo.getAge(), RESPONSE_AGE);
    assertEquals(pojo.getName(), RESPONSE_NAME);
  }
}
