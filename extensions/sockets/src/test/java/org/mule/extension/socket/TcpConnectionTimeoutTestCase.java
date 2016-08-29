/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket;

import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import org.mule.runtime.core.exception.MessagingException;

import java.net.SocketTimeoutException;

import org.junit.Test;

public class TcpConnectionTimeoutTestCase extends SocketExtensionTestCase {

  @Override
  protected String getConfigFile() {
    return "tcp-connection-timeout-config.xml";
  }

  @Test
  public void socketConnectionTimeout() throws Exception {
    final Throwable throwable = catchThrowable(() -> flowRunner("tcp-connection-timeout").withPayload(TEST_STRING).run());
    assertThat(throwable, is(instanceOf(MessagingException.class)));
    assertThat(((MessagingException) throwable).getCauseException(), is(instanceOf(SocketTimeoutException.class)));
  }
}
