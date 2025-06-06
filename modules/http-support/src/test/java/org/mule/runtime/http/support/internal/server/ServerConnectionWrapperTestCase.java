/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.server;

import static java.net.InetSocketAddress.createUnresolved;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.http.api.domain.request.ServerConnection;

import java.net.InetSocketAddress;

import org.junit.jupiter.api.Test;

class ServerConnectionWrapperTestCase {

  @Test
  void getLocalHostAddress() {
    InetSocketAddress address = createUnresolved("localhost", 1234);
    ServerConnection muleServerConnection = mock(ServerConnection.class);
    when(muleServerConnection.getLocalHostAddress()).thenReturn(address);
    ServerConnectionWrapper wrapper = new ServerConnectionWrapper(muleServerConnection);
    assertThat(wrapper.getLocalHostAddress(), is(address));
  }
}
