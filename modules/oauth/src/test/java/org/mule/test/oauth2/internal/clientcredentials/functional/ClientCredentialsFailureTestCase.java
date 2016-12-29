/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.clientcredentials.functional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.hamcrest.core.Is.isA;
import static org.mule.functional.junit4.matchers.ThrowableRootCauseMatcher.hasRootCause;

import org.mule.extension.oauth2.internal.TokenNotFoundException;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.oauth2.AbstractOAuthAuthorizationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import org.hamcrest.Matcher;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

@RunnerDelegateTo(Parameterized.class)
public class ClientCredentialsFailureTestCase extends AbstractOAuthAuthorizationTestCase {

  private static final String TOKEN_PATH = "/tokenUrl";
  private static final String TOKEN_PATH_PROPERTY_NAME = "token.url";

  @ClassRule
  public static DynamicPort dynamicPort = new DynamicPort("port");

  @Rule
  public SystemProperty tokenPathProp;
  @Rule
  public SystemProperty clientId = new SystemProperty("client.id", "ndli93xdws2qoe6ms1d389vl6bxquv3e");
  @Rule
  public SystemProperty clientSecret = new SystemProperty("client.secret", "yL692Az1cNhfk1VhTzyx4jOjjMKBrO9T");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(dynamicPort.getNumber()));

  @Override
  protected String getConfigFile() {
    return "client-credentials/client-credentials-minimal-config.xml";
  }

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[] {"tokenUrlFailsDuringAppStartup", (Consumer<WireMockRule>) wireMockRule -> {
    },
        format("http://localhost:%s%s", dynamicPort.getNumber() - 1, TOKEN_PATH),
        hasRootCause(isA(IOException.class))},
                         new Object[] {"accessTokenNotRetrieve", (Consumer<WireMockRule>) wireMockRule -> wireMockRule
                             .stubFor(post(urlEqualTo(TOKEN_PATH)).willReturn(aResponse().withBody(EMPTY))),
                             format("http://localhost:%s%s", dynamicPort.getNumber(), TOKEN_PATH),
                             hasRootCause(isA(TokenNotFoundException.class))});
  }

  public ClientCredentialsFailureTestCase(String name, Consumer<WireMockRule> wireMockConfigurer, String tokenPath,
                                          Matcher<? extends Throwable> expectedCauseMatcher) {
    wireMockConfigurer.accept(wireMockRule);
    tokenPathProp = new SystemProperty(TOKEN_PATH_PROPERTY_NAME, tokenPath);
    expectedException.expectCause(expectedCauseMatcher);
  }

  @Test
  public void runTest() {
    // Nothing to do here since the test subject is run during the setup.
  }
}
