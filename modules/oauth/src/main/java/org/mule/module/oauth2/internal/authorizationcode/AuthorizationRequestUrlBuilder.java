/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.authorizationcode;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import org.mule.module.oauth2.internal.OAuthConstants;
import org.mule.util.Preconditions;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds the authorization url to redirect the user to.
 */
public class AuthorizationRequestUrlBuilder
{

    private static final String ADDED_PARAMETER_TEMPLATE = "&%s=";

    private Logger logger = LoggerFactory.getLogger(AuthorizationRequestUrlBuilder.class);
    private String authorizationUrl;
    private String redirectUrl;
    private String clientId;
    private String scope;
    private String clientSecret;
    private Map<String, String> customParameters = new HashMap<String, String>();
    private String state;

    public AuthorizationRequestUrlBuilder setAuthorizationUrl(String authorizationUrl)
    {
        this.authorizationUrl = authorizationUrl;
        return this;
    }

    public AuthorizationRequestUrlBuilder setRedirectUrl(String redirectUrl)
    {
        this.redirectUrl = redirectUrl;
        return this;
    }

    public AuthorizationRequestUrlBuilder setClientId(String clientId)
    {
        this.clientId = clientId;
        return this;
    }

    public AuthorizationRequestUrlBuilder setClientSecret(String clientSecret)
    {
        this.clientSecret = clientSecret;
        return this;
    }

    public AuthorizationRequestUrlBuilder setScope(String scope)
    {
        this.scope = scope;
        return this;
    }

    public AuthorizationRequestUrlBuilder setCustomParameters(Map<String, String> customParameters)
    {
        this.customParameters = customParameters;
        return this;
    }

    /**
     * @return the authorization url with all the query parameters from the config.
     */
    public String buildUrl()
    {
        Preconditions.checkArgument(isNotBlank(clientId), "client cannot be blank");
        Preconditions.checkArgument(isNotBlank(clientSecret), "client cannot be blank");
        Preconditions.checkArgument(isNotBlank(authorizationUrl), "client cannot be blank");
        Preconditions.checkArgument(customParameters != null, "client cannot be null");
        return buildAuthorizeUrl();
    }

    private final String buildAuthorizeUrl()
    {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(authorizationUrl);

        urlBuilder.append("?")
                .append("response_type=code&")
                .append(OAuthConstants.CLIENT_ID_PARAMETER + "=")
                .append(clientId);

        try
        {
            if (isNotBlank(scope))
            {
                urlBuilder.append(String.format(ADDED_PARAMETER_TEMPLATE, OAuthConstants.SCOPE_PARAMETER)).append(URLEncoder.encode(scope, "UTF-8"));
            }
            if (isNotBlank(state))
            {
                urlBuilder.append(String.format(ADDED_PARAMETER_TEMPLATE, OAuthConstants.STATE_PARAMETER)).append(URLEncoder.encode(state, "UTF-8"));
            }

            for (Map.Entry<String, String> entry : customParameters.entrySet())
            {
                urlBuilder.append("&")
                        .append(entry.getKey())
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }

            urlBuilder.append(String.format(ADDED_PARAMETER_TEMPLATE, OAuthConstants.REDIRECT_URI_PARAMETER)).append(URLEncoder.encode(redirectUrl, "UTF-8"));
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }

        if (logger.isDebugEnabled())
        {
            logger.debug(("Authorization URL has been generated as follows: " + urlBuilder));
        }
        return urlBuilder.toString();
    }

    public AuthorizationRequestUrlBuilder setState(String state)
    {
        this.state = state;
        return this;
    }

}
