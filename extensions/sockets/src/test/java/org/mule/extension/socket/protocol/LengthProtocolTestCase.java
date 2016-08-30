/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.protocol;

import static org.hamcrest.Matchers.instanceOf;
import org.mule.extension.socket.SocketExtensionTestCase;
import org.mule.extension.socket.api.exceptions.LengthExceededException;
import org.mule.runtime.core.exception.MessagingException;

import org.junit.Test;

public class LengthProtocolTestCase extends SocketExtensionTestCase {

  public static final String LONG_TEST_STRING = "this is a long test string";
  public static final String SHORT_TEST_STRING = "stringy";

  @Override
  protected String getConfigFile() {
    return "length-protocol-config.xml";
  }

  @Test
  public void sendLongerMsg() throws Exception {
    expectedException.expect(MessagingException.class);
    expectedException.expectCause(instanceOf(LengthExceededException.class));
    flowRunner("tcp-send").withPayload(LONG_TEST_STRING).run();
  }

  @Test
  public void sendShorterMsg() throws Exception {
    flowRunner("tcp-send").withPayload(SHORT_TEST_STRING).run();
    assertEvent(receiveConnection(), SHORT_TEST_STRING);
  }
}
