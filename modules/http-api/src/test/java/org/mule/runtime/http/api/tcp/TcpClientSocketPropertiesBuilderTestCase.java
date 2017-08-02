/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.tcp;

import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_SERVICE;
import static org.mule.test.allure.AllureConstants.HttpFeature.HttpStory.TCP_BUILDER;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(HTTP_SERVICE)
@Story(TCP_BUILDER)
public class TcpClientSocketPropertiesBuilderTestCase {

  TcpClientSocketPropertiesBuilder builder = TcpClientSocketProperties.builder();

  @Test
  public void defaultConfiguration() {
    TcpClientSocketProperties properties = builder.build();
    assertThat(properties.getConnectionTimeout(), is(30000));
    assertThat(properties.getKeepAlive(), is(false));
    assertThat(properties.getSendTcpNoDelay(), is(true));
    assertThat(properties.getSendBufferSize(), is(nullValue()));
    assertThat(properties.getReceiveBufferSize(), is(nullValue()));
    assertThat(properties.getLinger(), is(nullValue()));
    assertThat(properties.getClientTimeout(), is(nullValue()));
  }

  @Test
  public void complexConfiguration() {
    TcpClientSocketProperties properties = builder
        .connectionTimeout(1)
        .keepAlive(true)
        .sendTcpNoDelay(false)
        .sendBufferSize(2)
        .receiveBufferSize(3)
        .linger(4)
        .clientTimeout(5).build();

    assertThat(properties.getConnectionTimeout(), is(1));
    assertThat(properties.getKeepAlive(), is(true));
    assertThat(properties.getSendTcpNoDelay(), is(false));
    assertThat(properties.getSendBufferSize(), is(2));
    assertThat(properties.getReceiveBufferSize(), is(3));
    assertThat(properties.getLinger(), is(4));
    assertThat(properties.getClientTimeout(), is(5));
  }

}
