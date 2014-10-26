/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.asserter;

import static org.junit.Assert.assertThat;

import org.mule.api.el.ExpressionLanguage;

import org.hamcrest.core.Is;

public class OAuthContextFunctionAsserter
{

    private final ExpressionLanguage expressionLanguage;
    private final String configName;
    private final String userId;

    private OAuthContextFunctionAsserter(ExpressionLanguage expressionLanguage, String configName, String userId)
    {
        this.expressionLanguage = expressionLanguage;
        this.configName = configName;
        this.userId = userId;
    }

    public static OAuthContextFunctionAsserter createFrom(ExpressionLanguage expressionLanguage, String configName)
    {
        return new OAuthContextFunctionAsserter(expressionLanguage, configName, null);
    }

    public static OAuthContextFunctionAsserter createFrom(ExpressionLanguage expressionLanguage, String configName, String userId)
    {
        return new OAuthContextFunctionAsserter(expressionLanguage, configName, userId);
    }

    public OAuthContextFunctionAsserter assertAccessTokenIs(String expectedAccessToken)
    {
        if (userId != null)
        {
            assertThat(expressionLanguage.evaluate(String.format("oauthContext('%s','%s').accessToken", configName, userId)), Is.<Object>is(expectedAccessToken));
        }
        else
        {
            assertThat(expressionLanguage.evaluate(String.format("oauthContext('%s').accessToken", configName)), Is.<Object>is(expectedAccessToken));
        }
        return this;
    }

    public OAuthContextFunctionAsserter assertRefreshTokenIs(String expectedRefreshToken)
    {
        if (userId != null)
        {
            assertThat(expressionLanguage.evaluate(String.format("oauthContext('%s','%s').refreshToken", configName, userId)), Is.<Object>is(expectedRefreshToken));
        }
        else
        {
            assertThat(expressionLanguage.evaluate(String.format("oauthContext('%s').refreshToken", configName)), Is.<Object>is(expectedRefreshToken));
        }
        return this;
    }

    public OAuthContextFunctionAsserter assertState(String expectedState)
    {
        if (userId != null)
        {
            assertThat(expressionLanguage.evaluate(String.format("oauthContext('%s','%s').state", configName, userId)), Is.<Object>is(expectedState));
        }
        else
        {
            assertThat(expressionLanguage.evaluate(String.format("oauthContext('%s').state", configName)), Is.<Object>is(expectedState));
        }
        return this;
    }

    public OAuthContextFunctionAsserter assertExpiresInIs(String expectedExpiresIs)
    {
        if (userId != null)
        {
            assertThat(expressionLanguage.evaluate(String.format("oauthContext('%s','%s').expiresIn", configName, userId)), Is.<Object>is(expectedExpiresIs));
        }
        else
        {
            assertThat(expressionLanguage.evaluate(String.format("oauthContext('%s').expiresIn", configName)), Is.<Object>is(expectedExpiresIs));
        }
        return this;
    }

    public OAuthContextFunctionAsserter assertContainsCustomTokenResponseParam(String paramName, String paramValue)
    {
        if (userId != null)
        {
            assertThat(expressionLanguage.evaluate(String.format("oauthContext('%s','%s').tokenResponseParameters['%s']", configName, userId, paramName)), Is.<Object>is(paramValue));
        }
        else
        {
            assertThat(expressionLanguage.evaluate(String.format("oauthContext('%s').tokenResponseParameters['%s']", configName, paramName)), Is.<Object>is(paramValue));
        }
        return this;
    }
}
