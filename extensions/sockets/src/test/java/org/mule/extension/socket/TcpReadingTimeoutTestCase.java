/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket;

import static org.hamcrest.Matchers.instanceOf;
import org.mule.runtime.core.exception.MessagingException;

import java.net.SocketTimeoutException;

import org.junit.Test;

public class TcpReadingTimeoutTestCase extends SocketExtensionTestCase {

  @Override
  protected String getConfigFile() {
    return "tcp-reading-timeout-config.xml";
  }

  @Test
  public void socketThrowsTimeout() throws Exception {
    expectedException.expect(MessagingException.class);
    expectedException.expectCause(instanceOf(SocketTimeoutException.class));

    flowRunner("tcp-reading-timeout").withPayload(TEST_STRING).run();
  }
}
