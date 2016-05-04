/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.authorizationcode.functional;

import org.mule.api.MessagingException;
import org.mule.module.oauth2.api.RequestAuthenticationException;
import org.mule.module.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AuthorizationCodeRefreshTokenConfigTestCase extends AbstractAuthorizationCodeRefreshTokenConfigTestCase
{

    public static final String SINGLE_TENANT_OAUTH_CONFIG = "oauthConfig";
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Override
    protected String getConfigFile()
    {
        return "authorization-code/authorization-code-refresh-token-config.xml";
    }

    @Test
    public void afterFailureDoRefreshTokenWithDefaultValueNoResourceOwnerId() throws Exception
    {
        executeRefreshToken("testFlow", SINGLE_TENANT_OAUTH_CONFIG, ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID, 403);
    }

    /**
     * Refresh token is optional therefore this test will validate an scenario where if the access_token is invalid and refresh_token
     * has not been provided in last refreshToken operation either token operation an {@link RequestAuthenticationException} should be thrown.
     * @throws Exception
     */
    @Test
    public void afterFailureWithRefreshTokenNotIssuedThrowAuthenticationException() throws Exception
    {
        expectedException.expect(MessagingException.class);
        expectedException.expectCause(Matchers.<Throwable>instanceOf(RequestAuthenticationException.class));
        executeRefreshTokenNotIssuedOnTokenCall("testFlow", SINGLE_TENANT_OAUTH_CONFIG, ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID, 403);
    }
}
