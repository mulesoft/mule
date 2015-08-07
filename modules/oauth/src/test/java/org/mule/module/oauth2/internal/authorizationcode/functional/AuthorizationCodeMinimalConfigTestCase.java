/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.authorizationcode.functional;

import org.mule.module.oauth2.asserter.OAuthContextFunctionAsserter;
import org.mule.module.oauth2.internal.OAuthConstants;

import org.apache.http.client.fluent.Request;
import org.junit.Ignore;
import org.junit.Test;

public class AuthorizationCodeMinimalConfigTestCase extends AbstractAuthorizationCodeBasicTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "authorization-code/authorization-code-minimal-config.xml";
    }

    @Ignore("MULE-6926: flaky test")
    @Test
    public void hitRedirectUrlAndGetToken() throws Exception
    {
        configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantType();

        Request.Get(redirectUrl.getValue() + "?" + OAuthConstants.CODE_PARAMETER + "=" + AUTHENTICATION_CODE)
                .connectTimeout(REQUEST_TIMEOUT)
                .socketTimeout(REQUEST_TIMEOUT)
                .execute();

        verifyRequestDoneToTokenUrlForAuthorizationCode();

        OAuthContextFunctionAsserter.createFrom(muleContext.getExpressionLanguage(), "tokenManagerConfig")
                .assertAccessTokenIs(ACCESS_TOKEN)
                .assertRefreshTokenIs(REFRESH_TOKEN);
    }

}
