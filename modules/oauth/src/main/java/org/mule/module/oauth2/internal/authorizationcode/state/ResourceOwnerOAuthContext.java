/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.authorizationcode.state;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;

/**
 * OAuth state for a particular resource owner which typically represents an user.
 */
public class ResourceOwnerOAuthContext implements Serializable
{

    public static final String DEFAULT_RESOURCE_OWNER_ID = "default";

    private final String resourceOwnerId;
    private transient Lock refreshUserOAuthContextLock;
    private String accessToken;
    private String refreshToken;
    private String state;
    private String expiresIn;
    private Map<String, Object> tokenResponseParameters = new HashMap<String, Object>();

    public ResourceOwnerOAuthContext(final Lock refreshUserOAuthContextLock, final String resourceOwnerId)
    {
        this.refreshUserOAuthContextLock = refreshUserOAuthContextLock;
        this.resourceOwnerId = resourceOwnerId;
    }

    /**
     * @return access token of the oauth context retrieved by the token request
     */
    public String getAccessToken()
    {
        return accessToken;
    }

    /**
     * @return refresh token of the oauth context retrieved by the token request
     */
    public String getRefreshToken()
    {
        return refreshToken;
    }

    /**
     * @return state of the oauth context send in the authorization request
     */
    public String getState()
    {
        return state;
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

    /**
     * @return expires in value retrieved by the token request.
     */
    public String getExpiresIn()
    {
        return expiresIn;
    }

    public void setState(final String state)
    {
        this.state = state;
    }

    /**
     * @return custom token request response parameters configured for extraction.
     */
    public Map<String, Object> getTokenResponseParameters()
    {
        return tokenResponseParameters;
    }

    public void setTokenResponseParameters(final Map<String, Object> tokenResponseParameters)
    {
        this.tokenResponseParameters = tokenResponseParameters;
    }

    /**
     * @return a lock that can be used to avoid concurrency problems trying to update oauth context.
     */
    public Lock getRefreshUserOAuthContextLock()
    {
        return refreshUserOAuthContextLock;
    }

    /**
     * @return id for the oauth state.
     */
    public String getResourceOwnerId()
    {
        return resourceOwnerId;
    }

    public void setRefreshUserOAuthContextLock(Lock refreshUserOAuthContextLock)
    {
        this.refreshUserOAuthContextLock = refreshUserOAuthContextLock;
    }
}
