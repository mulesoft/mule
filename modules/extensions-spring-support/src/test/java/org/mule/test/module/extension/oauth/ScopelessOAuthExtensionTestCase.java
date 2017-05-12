/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.extension.api.connectivity.oauth.AuthCodeRequest;

public class ScopelessOAuthExtensionTestCase extends OAuthExtensionTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"scopeless-oauth-extension-config.xml", "oauth-extension-flows.xml"};
  }

  @Override
  protected void assertScopes(AuthCodeRequest request) {
    assertThat(request.getScopes().isPresent(), is(false));
  }

  @Override
  protected void verifyAuthUrlRequest() {
    wireMock.verify(getRequestedFor(urlPathEqualTo("/" + AUTHORIZE_PATH))
        .withQueryParam("redirect_uri", equalTo((toUrl(CALLBACK_PATH, callbackPort.getNumber()))))
        .withQueryParam("client_id", equalTo(CONSUMER_KEY))
        .withQueryParam("state", containing(STATE)));
  }
}
