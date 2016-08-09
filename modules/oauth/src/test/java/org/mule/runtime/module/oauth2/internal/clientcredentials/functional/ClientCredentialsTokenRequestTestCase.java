/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.oauth2.internal.clientcredentials.functional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import org.mule.runtime.module.http.api.HttpHeaders;
import org.mule.runtime.module.oauth2.AbstractOAuthAuthorizationTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

public class ClientCredentialsTokenRequestTestCase extends AbstractOAuthAuthorizationTestCase {

  @Rule
  public SystemProperty tokenUrl =
      new SystemProperty("token.url", String.format("http://localhost:%d" + TOKEN_PATH, oauthServerPort.getNumber()));

  @Override
  protected String getConfigFile() {
    return "client-credentials/client-credentials-token-request-config.xml";
  }

  @Override
  public void doSetUpBeforeMuleContextCreation() {
    configureWireMockToExpectTokenPathRequestForClientCredentialsGrantTypeWithMapResponse(ACCESS_TOKEN);
    wireMockRule.stubFor(post(urlEqualTo("/resource")).withHeader(HttpHeaders.Names.AUTHORIZATION, containing(ACCESS_TOKEN))
        .willReturn(aResponse().withBody(TEST_MESSAGE).withStatus(200)));
  }


  @Test
  public void sendCredentialsInBody() throws Exception {
    testFlowAndExpectCredentialsInBody("credentialsInBody", true);
  }

  @Test
  public void sendCredentialsInHeader() throws Exception {
    testFlowAndExpectCredentialsInBody("credentialsInHeader", false);
  }

  private void testFlowAndExpectCredentialsInBody(String flowName, boolean credentialsInBody) throws Exception {
    flowRunner(flowName).withPayload(TEST_MESSAGE).run();
    verifyRequestDoneToTokenUrlForClientCredentials(scopes.getValue(), credentialsInBody);
  }
}
