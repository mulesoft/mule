/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.server;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.http.api.domain.request.ClientConnection;

import java.net.InetSocketAddress;
import java.security.cert.Certificate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClientConnectionWrapperTestCase {

  @Mock
  private ClientConnection muleConnection;

  private ClientConnectionWrapper connectionWrapper;

  @BeforeEach
  void setUp() {
    connectionWrapper = new ClientConnectionWrapper(muleConnection);
  }

  @Test
  void getRemoteHostAddress() {
    InetSocketAddress socketAddress = new InetSocketAddress("127.0.0.1", 8080);
    when(muleConnection.getRemoteHostAddress()).thenReturn(socketAddress);
    assertThat(muleConnection.getRemoteHostAddress(), is(socketAddress));
  }

  @Test
  void getConnection() {
    Certificate cert = mock(Certificate.class);
    when(muleConnection.getClientCertificate()).thenReturn(cert);
    assertThat(connectionWrapper.getClientCertificate(), is(cert));
  }
}
