/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket;

import static org.hamcrest.Matchers.instanceOf;
import org.mule.extension.socket.api.exceptions.ReadingTimeoutException;
import org.mule.runtime.core.exception.MessagingException;

import org.junit.Test;

public class UdpTimeoutTestCase extends SocketExtensionTestCase {

  @Override
  protected String getConfigFile() {
    return "udp-timeout-config.xml";
  }

  @Test
  public void socketThrowsTimeout() throws Exception {
    expectedException.expect(MessagingException.class);
    expectedException.expectCause(instanceOf(ReadingTimeoutException.class));

    flowRunner("udp-send-with-timeout").withPayload(TEST_STRING).run();
  }
}
