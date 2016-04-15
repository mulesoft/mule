/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.authorizationcode;

import org.mule.module.oauth2.internal.OAuthConstants;
import org.mule.module.oauth2.internal.ParameterExtractor;

import java.util.Collections;
import java.util.List;

/**
 * Provides configuration to handle a token url call response.
 */
public class TokenResponseConfiguration
{

    private String accessToken = OAuthConstants.ACCESS_TOKEN_EXPRESSION;
    private String refreshToken = OAuthConstants.REFRESH_TOKEN_EXPRESSION;
    private String expiresIn = OAuthConstants.EXPIRATION_TIME_EXPRESSION;

    private List<ParameterExtractor> parameterExtractors = Collections.emptyList();

    public void setParameterExtractors(final List<ParameterExtractor> parameterExtractors)
    {
        this.parameterExtractors = parameterExtractors;
    }

    public void setAccessToken(final String accessToken)
    {
        this.accessToken = accessToken;
    }

    public void setRefreshToken(final String refreshToken)
    {
        this.refreshToken = refreshToken;
    }

    public void setExpiresIn(final String expiresIn)
    {
        this.expiresIn = expiresIn;
    }

    public String getAccessToken()
    {
        return accessToken;
    }

    public String getRefreshToken()
    {
        return refreshToken;
    }

    public String getExpiresIn()
    {
        return expiresIn;
    }

    public List<ParameterExtractor> getParameterExtractors()
    {
        return parameterExtractors;
    }
}
