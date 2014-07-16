/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth;

import org.mule.api.NameableObject;
import org.mule.common.security.oauth.exception.UnableToAcquireAccessTokenException;
import org.mule.common.security.oauth.exception.UnableToAcquireRequestTokenException;

import java.util.Date;
import java.util.Map;
import java.util.regex.Pattern;

public interface OAuth2Adapter extends OAuthAdapter, OAuth2Connector, NameableObject
{

    /**
     * Build authorization URL and create the inbound endpoint for the callback
     * 
     * @param extraParameters Extra query string parameters that should be added to
     *            the authorization URL
     * @return The authorization URL
     */
    public String authorize(Map<String, String> extraParameters, String accessTokenUrl, String redirectUri)
        throws UnableToAcquireRequestTokenException;

    /**
     * Acquire access token and secret
     * 
     * @throws UnableToAcquireAccessTokenException
     */
    public void fetchAccessToken(String accessTokenUrl) throws UnableToAcquireAccessTokenException;

    public boolean hasTokenExpired();

    /**
     * @param accessTokenId
     * @throws UnableToAcquireAccessTokenException
     */
    public void refreshAccessToken(String accessTokenId) throws UnableToAcquireAccessTokenException;

    /**
     * Retrieve access token
     */
    public String getAccessToken();

    /**
     * Returns a compiled {@link java.util.regex.Pattern} which can be used to
     * extract the access code from a String
     */
    public Pattern getAccessCodePattern();

    /**
     * Retrieve refresh token
     */
    public String getRefreshToken();

    /**
     * Set refresh token
     */
    public void setRefreshToken(String refreshToken);

    /**
     * Returns a compiled {@link java.util.regex.Pattern} which can be used to
     * extract the refresh token from a String
     */
    public Pattern getRefreshTokenPattern();

    /**
     * Returns a compiled {@link java.util.regex.Pattern} which can be used to
     * extract the expiration time from a String
     */
    public Pattern getExpirationTimePattern();

    /**
     * Sets expiration
     */
    public void setExpiration(Date value);

    public void setConsumerKey(String consumerKey);

    public void setConsumerSecret(String consumerSecret);

}
