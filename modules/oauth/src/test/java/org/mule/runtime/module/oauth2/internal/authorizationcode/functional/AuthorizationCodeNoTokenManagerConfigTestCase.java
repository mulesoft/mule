/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.authorizationcode.functional;

import static org.junit.Assert.assertThat;

import org.mule.module.oauth2.internal.OAuthConstants;
import org.mule.module.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext;
import org.mule.module.oauth2.internal.tokenmanager.TokenManagerConfig;

import org.apache.http.client.fluent.Request;
import org.hamcrest.core.Is;
import org.junit.Test;

public class AuthorizationCodeNoTokenManagerConfigTestCase extends AbstractAuthorizationCodeBasicTestCase
{


    @Override
    protected String getConfigFile()
    {
        return "authorization-code/authorization-code-no-token-manager-config.xml";
    }

    @Test
    public void hitRedirectUrlAndGetToken() throws Exception
    {
        configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantType();

        Request.Get(redirectUrl.getValue() + "?" + OAuthConstants.CODE_PARAMETER + "=" + AUTHENTICATION_CODE)
                .connectTimeout(REQUEST_TIMEOUT)
                .socketTimeout(REQUEST_TIMEOUT)
                .execute();

        verifyRequestDoneToTokenUrlForAuthorizationCode();

        TokenManagerConfig tokenManagerConfig = muleContext.getRegistry().lookupObject(TokenManagerConfig.class);

        final ResourceOwnerOAuthContext oauthContext = tokenManagerConfig.getConfigOAuthContext().getContextForResourceOwner(ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID);

        assertThat(oauthContext.getAccessToken(), Is.is(ACCESS_TOKEN));
        assertThat(oauthContext.getRefreshToken(), Is.is(REFRESH_TOKEN));
    }

}
