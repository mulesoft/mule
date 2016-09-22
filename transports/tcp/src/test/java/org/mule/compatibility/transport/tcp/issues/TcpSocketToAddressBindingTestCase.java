/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp.issues;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.connector.DispatchException;
import org.mule.runtime.core.exception.MessagingException;

import java.net.InetAddress;
import java.net.SocketException;

import org.junit.Test;

/**
 * Tests how sockets are bound to addresses by the TCP transport. This test is related to MULE-6584.
 */
public class TcpSocketToAddressBindingTestCase extends AbstractTcpSocketToAddressBindingTestCase {

  public TcpSocketToAddressBindingTestCase() throws SocketException {
    super();
  }

  @Test
  public void testRequestUsingLoopbackAddressAtLoopbackAddress() throws Exception {
    MuleClient client = muleContext.getClient();
    InternalMessage result;

    // Request using loopback address at endpoint listening at 127.0.0.1 should get an appropiate response.
    result = client.send(getTransportName() + "://127.0.0.1:" + dynamicPort1.getNumber(), TEST_MESSAGE, null).getRight();
    assertThat(getPayloadAsString(result), is(TEST_MESSAGE + " Received"));
  }

  @Test
  public void testRequestUsingLocalhostAtLocalhost() throws Exception {
    MuleClient client = muleContext.getClient();
    InternalMessage result;

    // Request using localhost address at endpoint listening at localhost should get an appropiate response.
    result = client.send(getTransportName() + "://localhost:" + dynamicPort2.getNumber(), TEST_MESSAGE, null).getRight();
    assertThat(getPayloadAsString(result), is(TEST_MESSAGE + " Received"));
  }

  @Test
  public void testRequestUsingLoopbackAddressAtAllAddresses() throws Exception {
    MuleClient client = muleContext.getClient();
    InternalMessage result;

    // Request using loopback address at endpoint listening at all addresses should get an appropiate response.
    result = client.send(getTransportName() + "://127.0.0.1:" + dynamicPort3.getNumber(), TEST_MESSAGE, null).getRight();
    assertThat(getPayloadAsString(result), is(TEST_MESSAGE + " Received"));
  }

  @Test
  public void testRequestNotUsingLoopbackAddressAtLoopbackAddress() throws Exception {
    MuleClient client = muleContext.getClient();
    InternalMessage result;

    // Iterate over local addresses.
    for (InetAddress inetAddress : localInetAddresses) {
      // Request not using loopback address to endpoint listening at 127.0.0.1 should timeout.
      try {
        result = client.send(getTransportName() + "://" + inetAddress.getHostAddress() + ":" + dynamicPort1.getNumber(),
                             TEST_MESSAGE, null)
            .getRight();
        assertThat(result, nullValue());
      } catch (MessagingException ex) {
        assertThat(ex, hasCause(instanceOf(DispatchException.class)));
      }
    }
  }

  @Test
  public void testRequestNotUsingLoopbackAddressAtAllAddresses() throws Exception {
    MuleClient client = muleContext.getClient();
    InternalMessage result;

    // Iterate over local addresses.
    for (InetAddress inetAddress : localInetAddresses) {
      /*
       * Request not using loopback address to endpoint listening at all local addresses should get an appropriate response.
       */
      result = client.send(getTransportName() + "://" + inetAddress.getHostAddress() + ":" + dynamicPort3.getNumber(),
                           InternalMessage.of(TEST_MESSAGE))
          .getRight();
      assertThat(getPayloadAsString(result), is(TEST_MESSAGE + " Received"));
    }
  }
}
