/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth;

import org.mule.common.security.oauth.exception.NotAuthorizedException;

import java.io.Serializable;

public interface OAuthAdapter extends Serializable, OnNoTokenPolicyAware
{

    /**
     * Retrieve OAuth verifier
     * 
     * @return A String representing the OAuth verifier
     */
    public String getOauthVerifier();

    /**
     * Set OAuth verifier
     * 
     * @param value OAuth verifier to set
     */
    public void setOauthVerifier(String value);

    public void setAccessTokenUrl(String url);

    public void setAccessToken(String accessToken);

    public void setAuthorizationUrl(String authorizationUrl);

    /**
     * Checks if the adapter has been authorized. If it was not, then in trows
     * {@link org.mule.common.security.oauth.exception.NotAuthorizedException}
     * 
     * @throws NotAuthorizedException if the adapter hasn't been authorized
     */
    public void hasBeenAuthorized() throws NotAuthorizedException;

}
