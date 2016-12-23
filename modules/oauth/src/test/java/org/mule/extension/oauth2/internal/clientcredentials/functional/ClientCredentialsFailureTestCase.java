/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.clientcredentials.functional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.hamcrest.core.Is.isA;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.tck.MuleTestUtils.testWithSystemProperty;

import org.mule.extension.oauth2.internal.TokenNotFoundException;
import org.mule.functional.junit4.ApplicationContextBuilder;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.service.http.api.HttpService;
import org.mule.services.http.impl.service.HttpServiceImplementation;
import org.mule.tck.config.TestServicesConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class ClientCredentialsFailureTestCase extends AbstractMuleContextTestCase {

  private static final String TOKEN_PATH = "/tokenUrl";
  private static final String TOKEN_PATH_PROPERTY_NAME = "token.url";

  private DynamicPort dynamicPort = new DynamicPort("port");

  @Rule
  public SystemProperty clientId = new SystemProperty("client.id", "ndli93xdws2qoe6ms1d389vl6bxquv3e");
  @Rule
  public SystemProperty clientSecret = new SystemProperty("client.secret", "yL692Az1cNhfk1VhTzyx4jOjjMKBrO9T");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(dynamicPort.getNumber()));

  private HttpService httpService = new HttpServiceImplementation();

  @Before
  public void before() throws MuleException {
    startIfNeeded(httpService);
  }

  @After
  public void after() throws MuleException {
    stopIfNeeded(httpService);
  }

  @Test
  public void tokenUrlFailsDuringAppStartup() throws Exception {
    testWithSystemProperty(TOKEN_PATH_PROPERTY_NAME, "http://unkownhost:9999" + TOKEN_PATH, () -> {
      ApplicationContextBuilder applicationContextBuilder = new WithServicesApplicationContextBuilder(httpService)
          .setApplicationResources(new String[] {"client-credentials/client-credentials-minimal-config.xml"});
      expectedException.expectCause(isA(IOException.class));
      applicationContextBuilder.build();
    });
  }

  @Test
  public void accessTokenNotRetrieve() throws Exception {
    wireMockRule.stubFor(post(urlEqualTo(TOKEN_PATH)).willReturn(aResponse().withBody(EMPTY)));
    testWithSystemProperty(TOKEN_PATH_PROPERTY_NAME, format("http://localhost:%s%s", wireMockRule.port(), TOKEN_PATH), () -> {
      ApplicationContextBuilder applicationContextBuilder =
          new WithServicesApplicationContextBuilder(httpService).setApplicationResources(new String[] {
              "client-credentials/client-credentials-minimal-config.xml"});
      expectedException.expectCause(isA(TokenNotFoundException.class));
      applicationContextBuilder.build();
    });
  }

  private static class WithServicesApplicationContextBuilder extends ApplicationContextBuilder {

    private HttpService httpService;

    public WithServicesApplicationContextBuilder(HttpService httpService) {
      this.httpService = httpService;
    }

    @Override
    protected void addBuilders(List<ConfigurationBuilder> builders) {
      super.addBuilders(builders);
      builders.add(new TestServicesConfigurationBuilder());
      builders.add(new AbstractConfigurationBuilder() {

        @Override
        protected void doConfigure(MuleContext muleContext) throws Exception {
          muleContext.getRegistry().registerObject(httpService.getName(), httpService);
        }
      });
    }
  }
}
