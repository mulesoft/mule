/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.client.proxy;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_SERVICE;
import static org.mule.test.allure.AllureConstants.HttpFeature.HttpStory.PROXY_CONFIG_BUILDER;

import org.mule.runtime.http.api.client.proxy.ProxyConfig.NtlmProxyConfig;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(HTTP_SERVICE)
@Story(PROXY_CONFIG_BUILDER)
public class NtlmProxyConfigBuilderTest extends AbstractProxyConfigTestCase<NtlmProxyConfigBuilder> {

  private static final String NTLM_DOMAIN = "DOMAIN";

  private NtlmProxyConfigBuilder ntlmProxyConfigBuilder = NtlmProxyConfig.builder();

  @Override
  protected NtlmProxyConfigBuilder getProxyConfigBuilder() {
    return ntlmProxyConfigBuilder;
  }

  @Test
  public void onlyDomain() {
    expectedException.expect(IllegalArgumentException.class);
    ntlmProxyConfigBuilder.ntlmDomain(NTLM_DOMAIN).build();
  }

  @Test
  public void fullConfig() {
    NtlmProxyConfig config = ntlmProxyConfigBuilder.host(HOST).port(PORT).ntlmDomain(NTLM_DOMAIN)
        .username(USERNAME).password(PASSWORD).nonProxyHosts(NON_PROXY_HOSTS).build();
    assertThat(config.getHost(), is(HOST));
    assertThat(config.getPort(), is(PORT));
    assertThat(config.getNtlmDomain(), is(NTLM_DOMAIN));
    assertThat(config.getPassword(), is(PASSWORD));
    assertThat(config.getUsername(), is(USERNAME));
    assertThat(config.getNonProxyHosts(), is(NON_PROXY_HOSTS));
  }
}
