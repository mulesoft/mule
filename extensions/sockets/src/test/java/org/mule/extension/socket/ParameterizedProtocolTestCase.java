/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket;

import org.mule.extension.socket.api.connection.tcp.protocol.DirectProtocol;
import org.mule.extension.socket.api.connection.tcp.protocol.LengthProtocol;
import org.mule.extension.socket.api.connection.tcp.protocol.SafeProtocol;
import org.mule.extension.socket.api.socket.tcp.TcpProtocol;
import org.mule.functional.junit4.runners.RunnerDelegateTo;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized;

/**
 * Base clase for common tests across all the {@link TcpProtocol} implementations
 */
@RunnerDelegateTo(Parameterized.class)
public abstract class ParameterizedProtocolTestCase extends SocketExtensionTestCase {

  @Parameterized.Parameter(0)
  public String testName;

  @Parameterized.Parameter(1)
  public String protocolBeanName;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{LengthProtocol.class.getSimpleName(), "length"},
        {DirectProtocol.class.getSimpleName(), "direct"}, {SafeProtocol.class.getSimpleName(), "safe"},});
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    super.doSetUpBeforeMuleContextCreation();
    System.setProperty("protocol", protocolBeanName);
  }

  @Override
  protected void doTearDown() throws Exception {
    System.clearProperty("protocol");
    super.doTearDown();
  }

}
