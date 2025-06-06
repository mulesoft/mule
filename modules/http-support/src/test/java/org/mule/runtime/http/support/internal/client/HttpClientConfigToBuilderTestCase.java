/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.http.api.client.HttpClientConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HttpClientConfigToBuilderTestCase {

  private HttpClientConfiguration.Builder actualBuilder;

  @Mock
  private TlsContextFactory tlsContextFactory;

  private HttpClientConfigToBuilder configurer;

  @BeforeEach
  void setUp() {
    actualBuilder = new HttpClientConfiguration.Builder();
    configurer = new HttpClientConfigToBuilder(actualBuilder);
  }

  @Test
  void nameIsMandatory() {
    var exceptionWhenNoName = assertThrows(NullPointerException.class, actualBuilder::build);
    assertThat(exceptionWhenNoName.getMessage(), containsString("Name is mandatory"));
  }

  @Test
  void testSetName() {
    String name = "test-client";
    configurer.setName(name);

    HttpClientConfiguration config = actualBuilder.build();
    assertThat(config.getName(), is(name));
  }

  @Test
  void setTlsContextFactory() {
    configurer
        .setName("test-client")
        .setTlsContextFactory(tlsContextFactory);

    HttpClientConfiguration config = actualBuilder.build();
    assertThat(config.getTlsContextFactory(), is(tlsContextFactory));
  }

  @ParameterizedTest
  @ValueSource(ints = {10, 20})
  void setMaxConnections(int maxConnections) {
    configurer
        .setName("test-client")
        .setMaxConnections(maxConnections);

    HttpClientConfiguration config = actualBuilder.build();
    assertThat(config.getMaxConnections(), is(maxConnections));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void setUsePersistentConnections(boolean usePersistentConnections) {
    configurer
        .setName("test-client")
        .setUsePersistentConnections(usePersistentConnections);

    HttpClientConfiguration config = actualBuilder.build();
    assertThat(config.isUsePersistentConnections(), is(usePersistentConnections));
  }

  @ParameterizedTest
  @ValueSource(ints = {1024, 2048})
  void setConnectionIdleTimeout(int timeout) {
    configurer
        .setName("test-client")
        .setConnectionIdleTimeout(timeout);

    HttpClientConfiguration config = actualBuilder.build();
    assertThat(config.getConnectionIdleTimeout(), is(timeout));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void setStreaming(boolean streaming) {
    configurer
        .setName("test-client")
        .setStreaming(streaming);

    HttpClientConfiguration config = actualBuilder.build();
    assertThat(config.isStreaming(), is(streaming));
  }

  @ParameterizedTest
  @ValueSource(ints = {1024, 2048})
  void setResponseBufferSize(int bufferSize) {
    configurer
        .setName("test-client")
        .setResponseBufferSize(bufferSize);

    HttpClientConfiguration config = actualBuilder.build();
    assertThat(config.getResponseBufferSize(), is(bufferSize));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testSetDecompress(boolean decompress) {
    configurer
        .setName("test-client")
        .setDecompress(decompress);

    HttpClientConfiguration config = actualBuilder.build();
    assertThat(config.isDecompress(), is(decompress));
  }

  @Test
  void testConfigClientSocketProperties() {
    int sendBufferSize = 1234;
    int linger = 50;
    configurer
        .setName("test-client")
        .configClientSocketProperties(config -> {
          config.sendBufferSize(sendBufferSize);
          config.linger(linger);
        });

    var config = actualBuilder.build();
    var socketProperties = config.getClientSocketProperties();
    assertThat(socketProperties.getSendBufferSize(), is(sendBufferSize));
    assertThat(socketProperties.getLinger(), is(linger));
  }

  @Test
  void testConfigProxy() {
    String proxyHost = "proxy.example.com";
    int proxyPort = 1234;
    String proxyUser = "user";
    String proxyPassword = "password";
    configurer
        .setName("test-client")
        .configProxy(config -> config
            .host(proxyHost).port(proxyPort)
            .auth(auth -> auth
                .basic(proxyUser, proxyPassword, false)));

    var config = actualBuilder.build();
    var proxyConfig = config.getProxyConfig();
    assertThat(proxyConfig.getHost(), is(proxyHost));
    assertThat(proxyConfig.getUsername(), is(proxyUser));
    assertThat(proxyConfig.getPassword(), is(proxyPassword));
    assertThat(proxyConfig.getPort(), is(proxyPort));
  }
}
