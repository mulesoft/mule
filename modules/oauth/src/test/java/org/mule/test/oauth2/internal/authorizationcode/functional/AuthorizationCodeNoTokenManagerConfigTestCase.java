/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.authorizationcode.functional;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.oauth2.internal.tokenmanager.TokenManagerConfig.defaultTokenManagerConfigIndex;

import org.mule.extension.oauth2.internal.OAuthConstants;
import org.mule.extension.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext;
import org.mule.extension.oauth2.internal.tokenmanager.TokenManagerConfig;

import org.apache.http.client.fluent.Request;
import org.junit.Test;

public class AuthorizationCodeNoTokenManagerConfigTestCase extends AbstractAuthorizationCodeBasicTestCase {


  @Override
  protected String getConfigFile() {
    return "authorization-code/authorization-code-no-token-manager-config.xml";
  }

  @Test
  public void hitRedirectUrlAndGetToken() throws Exception {
    configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantType();

    Request.Get(localCallbackUrl.getValue() + "?" + OAuthConstants.CODE_PARAMETER + "=" + AUTHENTICATION_CODE)
        .connectTimeout(REQUEST_TIMEOUT).socketTimeout(REQUEST_TIMEOUT).execute();

    verifyRequestDoneToTokenUrlForAuthorizationCode();

    TokenManagerConfig tokenManagerConfig =
        muleContext.getRegistry().get("default-token-manager-config-" + (defaultTokenManagerConfigIndex.get() - 1));

    final ResourceOwnerOAuthContext oauthContext = tokenManagerConfig.getConfigOAuthContext()
        .getContextForResourceOwner(ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID);

    assertThat(oauthContext.getAccessToken(), is(ACCESS_TOKEN));
    assertThat(oauthContext.getRefreshToken(), is(REFRESH_TOKEN));
  }

}
