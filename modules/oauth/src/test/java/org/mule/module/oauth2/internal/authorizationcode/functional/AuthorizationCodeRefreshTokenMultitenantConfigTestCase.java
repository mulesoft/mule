/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.authorizationcode.functional;

import org.mule.module.oauth2.asserter.OAuthContextFunctionAsserter;
import org.mule.module.oauth2.internal.authorizationcode.AuthorizationCodeConfig;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

public class AuthorizationCodeRefreshTokenMultitenantConfigTestCase extends AbstractAuthorizationCodeRefreshTokenConfigTestCase
{

    public static final String MULTITENANT_OAUTH_CONFIG = "multitenantOauthConfig";

    public static final String USER_ID_JOHN = "john";
    public static final String JOHN_ACCESS_TOKEN = "123456789";
    public static final String USER_ID_TONY = "tony";
    public static final String TONY_ACCESS_TOKEN = "abcdefghi";

    @Rule
    public SystemProperty multitenantUser = new SystemProperty("multitenant.user", "john");

    @Override
    protected String getConfigFile()
    {
        return "authorization-code/authorization-code-refresh-token-config-with-resource-owner.xml";
    }

    @Test
    public void afterFailureDoRefreshTokenWithCustomValueWithResourceOwnerId() throws Exception
    {
        final AuthorizationCodeConfig contextOAuthState = muleContext.getRegistry().lookupObject(MULTITENANT_OAUTH_CONFIG);
        contextOAuthState.getConfigOAuthContext().getContextForUser(USER_ID_TONY).setAccessToken(TONY_ACCESS_TOKEN);
        contextOAuthState.getConfigOAuthContext().getContextForUser(USER_ID_JOHN).setAccessToken(JOHN_ACCESS_TOKEN);

        executeRefreshToken("testMultitenantFlow", MULTITENANT_OAUTH_CONFIG, multitenantUser.getValue(), 500);

        OAuthContextFunctionAsserter.createFrom(muleContext.getExpressionLanguage(), MULTITENANT_OAUTH_CONFIG, USER_ID_JOHN)
                .assertAccessTokenIs(REFRESHED_ACCESS_TOKEN)
                .assertState(null);
        OAuthContextFunctionAsserter.createFrom(muleContext.getExpressionLanguage(), MULTITENANT_OAUTH_CONFIG, USER_ID_TONY)
                .assertAccessTokenIs(TONY_ACCESS_TOKEN)
                .assertState(null);
    }

}
