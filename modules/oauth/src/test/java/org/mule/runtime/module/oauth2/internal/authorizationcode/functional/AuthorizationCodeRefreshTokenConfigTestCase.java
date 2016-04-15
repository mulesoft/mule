/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.authorizationcode.functional;

import org.mule.module.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext;

import org.junit.Test;

public class AuthorizationCodeRefreshTokenConfigTestCase extends AbstractAuthorizationCodeRefreshTokenConfigTestCase
{

    public static final String SINGLE_TENANT_OAUTH_CONFIG = "oauthConfig";

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

}
