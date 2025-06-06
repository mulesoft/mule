/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.server;

import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_FORWARD_COMPATIBILITY;

import static java.net.InetAddress.getByName;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.http.api.server.ServerAddress;

import java.net.InetAddress;
import java.net.UnknownHostException;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Feature(HTTP_FORWARD_COMPATIBILITY)
@ExtendWith(MockitoExtension.class)
class ServerAddressWrapperTestCase {

  @Mock
  private ServerAddress mockAddress;

  private ServerAddressWrapper addressWrapper;

  @BeforeEach
  void setUp() {
    addressWrapper = new ServerAddressWrapper(mockAddress);
  }

  @Test
  void getPort() {
    when(mockAddress.getPort()).thenReturn(12345);
    assertThat(addressWrapper.getPort(), is(12345));
  }

  @Test
  void getIp() {
    when(mockAddress.getIp()).thenReturn("1.2.3.4");
    assertThat(addressWrapper.getIp(), is("1.2.3.4"));
  }

  @Test
  void getAddress() throws UnknownHostException {
    InetAddress address = getByName("1.2.3.4");
    when(mockAddress.getAddress()).thenReturn(address);
    assertThat(addressWrapper.getAddress(), is(address));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void overlaps(boolean shouldOverlap) {
    ServerAddress anotherAddress = mock(ServerAddress.class);
    ServerAddressWrapper anotherWrapper = new ServerAddressWrapper(anotherAddress);
    when(mockAddress.overlaps(anotherAddress)).thenReturn(shouldOverlap);

    assertThat(addressWrapper.overlaps(anotherWrapper), is(shouldOverlap));
  }
}
