/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import org.mule.runtime.http.api.client.HttpRequestOptions;
import org.mule.runtime.http.api.client.HttpRequestOptionsBuilder;
import org.mule.runtime.http.api.client.auth.HttpAuthentication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HttpRequestOptionsConfigToBuilderTestCase {

  private HttpRequestOptionsBuilder actualBuilder;

  private HttpRequestOptionsConfigToBuilder configurer;

  @BeforeEach
  void setUp() {
    actualBuilder = HttpRequestOptions.builder();
    configurer = new HttpRequestOptionsConfigToBuilder(actualBuilder);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void configSendBodyAlways(boolean sendBodyAlways) {
    configurer.setSendBodyAlways(sendBodyAlways);
    var options = actualBuilder.build();
    assertThat(options.shouldSendBodyAlways(), is(sendBodyAlways));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void configFollowsRedirect(boolean followsRedirect) {
    configurer.setFollowsRedirect(followsRedirect);
    var options = actualBuilder.build();
    assertThat(options.isFollowsRedirect(), is(followsRedirect));
  }

  @ParameterizedTest
  @ValueSource(ints = {10_000, 20_000, 30_000})
  void configResponseTimeout(int responseTimeout) {
    configurer.setResponseTimeout(responseTimeout);
    var options = actualBuilder.build();
    assertThat(options.getResponseTimeout(), is(responseTimeout));
  }

  @Test
  void configAuthentication() {
    String userName = "user";
    String password = "password";
    boolean isPreemptive = true;
    String domain = "domain";
    String workstation = "workstation";

    configurer.setAuthentication(auth -> auth.ntlm(userName, password, isPreemptive, domain, workstation));

    var options = actualBuilder.build();
    assertThat(options.getAuthentication().isPresent(), is(true));

    var auth = options.getAuthentication().get();
    assertThat(auth, instanceOf(HttpAuthentication.HttpNtlmAuthentication.class));

    var ntlmAuth = (HttpAuthentication.HttpNtlmAuthentication) auth;
    assertThat(ntlmAuth.getUsername(), is(userName));
    assertThat(ntlmAuth.getPassword(), is(password));
    assertThat(ntlmAuth.isPreemptive(), is(isPreemptive));
    assertThat(ntlmAuth.getDomain(), is(domain));
    assertThat(ntlmAuth.getWorkstation(), is(workstation));
  }

  @Test
  void configProxy() {
    String host = "proxy.host";
    int port = 12345;
    String nonProxyHost = "nonProxyHost";
    String username = "user";
    String password = "password";
    boolean isPreemptive = false;

    configurer.setProxyConfig(proxyConfig -> proxyConfig
        .host(host).port(port)
        .nonProxyHosts(nonProxyHost)
        .auth(auth -> auth
            .basic(username, password, isPreemptive)));

    var options = actualBuilder.build();
    assertThat(options.getProxyConfig().isPresent(), is(true));
    var proxyConfig = options.getProxyConfig().get();
    assertThat(proxyConfig.getHost(), is(host));
    assertThat(proxyConfig.getPort(), is(port));
    assertThat(proxyConfig.getNonProxyHosts(), is(nonProxyHost));
    assertThat(proxyConfig.getUsername(), is(username));
    assertThat(proxyConfig.getPassword(), is(password));
  }
}
