/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.authorizationcode.functional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.oauth2.AbstractOAuthAuthorizationTestCase;
import org.mule.test.oauth2.asserter.AuthorizationRequestAsserter;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import java.util.List;

import org.apache.http.client.fluent.Request;
import org.junit.Rule;
import org.junit.Test;

public abstract class AbstractAuthorizationCodeBasicTestCase extends AbstractOAuthAuthorizationTestCase {

  @Rule
  public SystemProperty localAuthorizationUrl =
      new SystemProperty("local.authorization.url",
                         String.format("%s://localhost:%d/authorization", getProtocol(), localHostPort.getNumber()));

  @Rule
  public SystemProperty authorizationUrl =
      new SystemProperty("authorization.url",
                         String.format("%s://localhost:%d" + AUTHORIZE_PATH, getProtocol(), resolveOauthServerPort()));

  private int resolveOauthServerPort() {
    return getProtocol().equals("http") ? oauthServerPort.getNumber() : oauthHttpsServerPort.getNumber();
  }

  @Rule
  public SystemProperty tokenUrl =
      new SystemProperty("token.url", String.format("%s://localhost:%d" + TOKEN_PATH, getProtocol(), resolveOauthServerPort()));

  protected String getProtocol() {
    return "http";
  }

  @Test
  public void localAuthorizationUrlRedirectsToOAuthAuthorizationUrl() throws Exception {
    LoggedRequest request = getLoggedRequest();

    AuthorizationRequestAsserter.create(request).assertMethodIsGet().assertClientIdIs(clientId.getValue())
        .assertRedirectUriIs(localCallbackUrl.getValue()).assertResponseTypeIsCode();
  }

  protected LoggedRequest getLoggedRequest() throws Exception {
    wireMockRule.stubFor(get(urlMatching(AUTHORIZE_PATH + ".*")).willReturn(aResponse().withStatus(200)));

    Request.Get(localAuthorizationUrl.getValue()).connectTimeout(RECEIVE_TIMEOUT).socketTimeout(RECEIVE_TIMEOUT).execute();

    final List<LoggedRequest> requests = findAll(getRequestedFor(urlMatching(AUTHORIZE_PATH + ".*")));
    assertThat(requests.size(), is(1));

    return requests.get(0);
  }
}
