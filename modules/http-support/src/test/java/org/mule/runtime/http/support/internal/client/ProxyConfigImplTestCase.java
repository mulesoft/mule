/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.client;

import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_FORWARD_COMPATIBILITY;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.mule.runtime.http.api.client.proxy.ProxyConfig.NtlmProxyConfig;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.Test;

@Feature(HTTP_FORWARD_COMPATIBILITY)
class ProxyConfigImplTestCase {

  private static final String PROXY_HOST = "localhost";
  private static final int PROXY_PORT = 7777;
  private static final String NON_PROXY_HOST = "non-proxy-host";
  private static final String PROXY_USERNAME = "Quiroga";
  private static final String PROXY_PASSWORD = "Capiangos";
  private static final String PROXY_DOMAIN = "Punzó";

  private final ProxyConfigImpl configurer = new ProxyConfigImpl();

  @Test
  void proxyWithBasicAuth() {
    configurer
        .host(PROXY_HOST).port(PROXY_PORT)
        .nonProxyHosts(NON_PROXY_HOST)
        .auth(auth -> auth.basic(PROXY_USERNAME, PROXY_PASSWORD, false));

    var proxyConfig = configurer.build();
    assertThat(proxyConfig.getHost(), is(PROXY_HOST));
    assertThat(proxyConfig.getPort(), is(PROXY_PORT));
    assertThat(proxyConfig.getNonProxyHosts(), is(NON_PROXY_HOST));
    assertThat(proxyConfig.getUsername(), is(PROXY_USERNAME));
    assertThat(proxyConfig.getPassword(), is(PROXY_PASSWORD));
  }

  @Test
  void proxyWithBasicAuthHasToBeNonPreemptive() {
    var exception = assertThrows(IllegalArgumentException.class,
                                 () -> configurer.auth(auth -> auth.basic(PROXY_USERNAME, PROXY_PASSWORD, true)));
    assertThat(exception.getMessage(), is("Preemptive basic authentication is not supported for proxy"));
  }

  @Test
  void digestIsNotAllowedForProxy() {
    var exception = assertThrows(IllegalArgumentException.class,
                                 () -> configurer.auth(auth -> auth.digest(PROXY_USERNAME, PROXY_PASSWORD, false)));
    assertThat(exception.getMessage(), is("Digest authentication is not supported for proxy"));
  }

  @Test
  void proxyWithNTLMAuth() {
    configurer
        .host(PROXY_HOST).port(PROXY_PORT)
        .nonProxyHosts(NON_PROXY_HOST)
        .auth(auth -> auth.ntlm(PROXY_USERNAME, PROXY_PASSWORD, false, PROXY_DOMAIN, null));

    var proxyConfig = configurer.build();
    assertThat(proxyConfig, instanceOf(NtlmProxyConfig.class));
    var ntlmProxyConfig = (NtlmProxyConfig) proxyConfig;

    assertThat(ntlmProxyConfig.getHost(), is(PROXY_HOST));
    assertThat(ntlmProxyConfig.getPort(), is(PROXY_PORT));
    assertThat(ntlmProxyConfig.getNonProxyHosts(), is(NON_PROXY_HOST));
    assertThat(ntlmProxyConfig.getUsername(), is(PROXY_USERNAME));
    assertThat(ntlmProxyConfig.getPassword(), is(PROXY_PASSWORD));
    assertThat(ntlmProxyConfig.getNtlmDomain(), is(PROXY_DOMAIN));
  }

  @Test
  void ntlmProxyCannotBePreemptiveNorHaveWorkstation() {
    var errorOnPreemptive =
        assertThrows(IllegalArgumentException.class,
                     () -> configurer.auth(auth -> auth.ntlm(PROXY_USERNAME, PROXY_PASSWORD, true, PROXY_DOMAIN, null)));
    assertThat(errorOnPreemptive.getMessage(), is("Preemptive NTLM authentication is not supported for proxy"));

    var errorOnWorkstation = assertThrows(IllegalArgumentException.class, () -> configurer
        .auth(auth -> auth.ntlm(PROXY_USERNAME, PROXY_PASSWORD, false, PROXY_DOMAIN, "Wrongstation")));
    assertThat(errorOnWorkstation.getMessage(), is("NTLM workstation can't be configured for proxy"));
  }
}
