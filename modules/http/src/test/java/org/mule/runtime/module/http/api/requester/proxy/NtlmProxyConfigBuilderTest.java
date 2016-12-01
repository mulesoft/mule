/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.api.requester.proxy;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import org.mule.runtime.module.http.internal.request.NtlmProxyConfig;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class NtlmProxyConfigBuilderTest extends AbstractMuleTestCase {

  public static final String HOST = "host";
  public static final int PORT = 8080;
  public static final String NTLM_DOMAIN = "DOMAIN";
  public static final String USERNAME = "username";
  public static final String PASSWORD = "password";

  private NtlmProxyConfigBuilder ntlmProxyConfigBuilder = new NtlmProxyConfigBuilder();

  @Test(expected = IllegalArgumentException.class)
  public void onlyHost() {
    ntlmProxyConfigBuilder.setHost(HOST).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void onlyPort() {
    ntlmProxyConfigBuilder.setPort(PORT).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void onlyDomain() {
    ntlmProxyConfigBuilder.setNtlmDomain(NTLM_DOMAIN).build();
  }

  @Test
  public void minimalConfig() {
    NtlmProxyConfig config =
        (NtlmProxyConfig) ntlmProxyConfigBuilder.setHost(HOST).setPort(PORT).setNtlmDomain(NTLM_DOMAIN).build();
    assertThat(config.getHost(), is(HOST));
    assertThat(config.getPort(), is(PORT));
    assertThat(config.getNtlmDomain(), is(NTLM_DOMAIN));
    assertThat(config.getPassword(), nullValue());
    assertThat(config.getUsername(), nullValue());
  }

  @Test
  public void fullConfig() {
    NtlmProxyConfig config = (NtlmProxyConfig) ntlmProxyConfigBuilder.setHost(HOST).setPort(PORT).setNtlmDomain(NTLM_DOMAIN)
        .setUsername(USERNAME).setPassword(PASSWORD).build();
    assertThat(config.getHost(), is(HOST));
    assertThat(config.getPort(), is(PORT));
    assertThat(config.getNtlmDomain(), is(NTLM_DOMAIN));
    assertThat(config.getPassword(), is(PASSWORD));
    assertThat(config.getUsername(), is(USERNAME));
  }
}
