/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.client.proxy;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_SERVICE;
import static org.mule.test.allure.AllureConstants.HttpFeature.HttpStory.PROXY_CONFIG_BUILDER;

import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(HTTP_SERVICE)
@Stories(PROXY_CONFIG_BUILDER)
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
    ntlmProxyConfigBuilder.setNtlmDomain(NTLM_DOMAIN).build();
  }

  @Test
  public void fullConfig() {
    NtlmProxyConfig config = ntlmProxyConfigBuilder.setHost(HOST).setPort(PORT).setNtlmDomain(NTLM_DOMAIN)
        .setUsername(USERNAME).setPassword(PASSWORD).build();
    assertThat(config.getHost(), is(HOST));
    assertThat(config.getPort(), is(PORT));
    assertThat(config.getNtlmDomain(), is(NTLM_DOMAIN));
    assertThat(config.getPassword(), is(PASSWORD));
    assertThat(config.getUsername(), is(USERNAME));
  }
}
