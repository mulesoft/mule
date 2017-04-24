/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.authorizationcode.functional;

import static java.lang.String.format;
import static org.apache.http.client.fluent.Request.Get;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;
import static org.mule.service.http.api.utils.HttpEncoderDecoderUtils.appendQueryParam;
import static org.mule.services.oauth.internal.OAuthConstants.ACCESS_TOKEN_PARAMETER;
import static org.mule.services.oauth.internal.OAuthConstants.CODE_PARAMETER;
import static org.mule.services.oauth.internal.OAuthConstants.EXPIRES_IN_PARAMETER;
import static org.mule.services.oauth.internal.OAuthConstants.REFRESH_TOKEN_PARAMETER;
import static org.mule.services.oauth.internal.OAuthConstants.STATE_PARAMETER;

import org.mule.extension.oauth2.internal.tokenmanager.TokenManagerConfig;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

/**
 * This test checks that we can extract tokens from a text response (when the server omits a mime-type or uses text/plain),
 * as the default now is to read from a JSON.
 */
public class AuthorizationCodeAuthorizationCustomTokenExprTestCase extends AbstractAuthorizationCodeBasicTestCase {

  @Rule
  public DynamicPort onCompleteUrlPort = new DynamicPort("onCompleteUrlPort");

  @Override
  protected String getConfigFile() {
    return "authorization-code/authorization-code-custom-token-expr-config.xml";
  }

  @Test
  public void callToTokenUrlSuccess() throws Exception {
    configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantType(ACCESS_TOKEN, REFRESH_TOKEN);
    Get(getRedirectUrlWithOnCompleteUrlAndCodeQueryParams()).connectTimeout(REQUEST_TIMEOUT).socketTimeout(REQUEST_TIMEOUT)
        .execute();
    final TokenManagerConfig tokenManagerConfig = muleContext.getRegistry().lookupObject(TokenManagerConfig.class);
    final ResourceOwnerOAuthContext oauthContext =
        tokenManagerConfig.getConfigOAuthContext().getContextForResourceOwner(DEFAULT_RESOURCE_OWNER_ID);

    assertThat(oauthContext.getAccessToken(), is(ACCESS_TOKEN));
    assertThat(oauthContext.getRefreshToken(), is(REFRESH_TOKEN));
  }

  private String getRedirectUrlWithOnCompleteUrlQueryParam() {
    return appendQueryParam(localCallbackUrl.getValue(), STATE_PARAMETER,
                            ":onCompleteRedirectTo=" + format("http://localhost:%s/afterLogin", onCompleteUrlPort.getNumber()));
  }

  private String getRedirectUrlWithOnCompleteUrlAndCodeQueryParams() {
    return appendQueryParam(getRedirectUrlWithOnCompleteUrlQueryParam(), CODE_PARAMETER, AUTHENTICATION_CODE);
  }

  @Override
  protected void configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantType(String accessToken, String refreshToken) {
    configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantTypeWithBody("{" + "\""
        + ACCESS_TOKEN_PARAMETER + "\":\"" + accessToken + "\"," + "\"" + EXPIRES_IN_PARAMETER
        + "\":" + EXPIRES_IN + "," + "\"" + REFRESH_TOKEN_PARAMETER + "\":\"" + refreshToken + "\"}",
                                                                                   MediaType.TEXT.toRfcString());
  }
}
