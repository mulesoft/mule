/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp.issues;

import static org.junit.Assert.assertEquals;

import org.mule.compatibility.transport.tcp.TcpPropertyHelper;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.rule.SystemProperty;

import java.net.InetAddress;
import java.net.SocketException;

import org.junit.Rule;
import org.junit.Test;

/**
 * Tests socket binding with legacy behaviour setting the MULE_TCP_BIND_LOCALHOST_TO_ALL_LOCAL_INTERFACES_PROPERTY to true. This
 * test can be deleted once the legacy behaviour is not supported any longer.
 */
public class TcpSocketToAddressLegacyBindingTestCase extends AbstractTcpSocketToAddressBindingTestCase {

  @Rule
  public SystemProperty bindLocalhostToAllLocalInterfaces =
      new SystemProperty(TcpPropertyHelper.MULE_TCP_BIND_LOCALHOST_TO_ALL_LOCAL_INTERFACES_PROPERTY, "true");

  public TcpSocketToAddressLegacyBindingTestCase() throws SocketException {
    super();
  }

  @Test
  public void testRequestNotUsingLoopbackAddressAtLocalhost() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage result;

    // Iterate over local addresses.
    for (InetAddress inetAddress : localInetAddresses) {
      /*
       * Request not using loopback address to endpoint listening at all local addresses should get an appropriate response.
       */
      result = client.send(getTransportName() + "://" + inetAddress.getHostAddress() + ":" + dynamicPort2.getNumber(),
                           TEST_MESSAGE, null)
          .getRight();
      assertEquals(TEST_MESSAGE + " Received", getPayloadAsString(result));
    }
  }
}
