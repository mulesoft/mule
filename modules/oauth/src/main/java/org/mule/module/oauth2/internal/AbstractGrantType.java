/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal;

import org.mule.module.http.api.HttpAuthentication;
import org.mule.module.http.api.requester.proxy.ProxyConfig;
import org.mule.module.oauth2.internal.authorizationcode.state.ConfigOAuthContext;
import org.mule.module.oauth2.internal.tokenmanager.TokenManagerConfig;
import org.mule.transport.ssl.api.TlsContextFactory;

/**
 * Common interface for all grant types must extend this interface.
 */
public abstract class AbstractGrantType implements HttpAuthentication, ApplicationCredentials
{

    protected ProxyConfig proxyConfig;
    protected TokenManagerConfig tokenManagerConfig;
    protected TlsContextFactory tlsContextFactory;
    protected String clientId;
    protected String clientSecret;

    /**
     * @param accessToken an ouath access token
     * @return the content of the HTTP authentication header.
     */
    public static String buildAuthorizationHeaderContent(String accessToken)
    {
        return "Bearer " + accessToken;
    }

    public void setProxyConfig(ProxyConfig proxyConfig)
    {
        this.proxyConfig = proxyConfig;
    }

    public TlsContextFactory getTlsContext()
    {
        return tlsContextFactory;
    }

    public void setTokenManager(TokenManagerConfig tokenManagerConfig)
    {
        this.tokenManagerConfig = tokenManagerConfig;
    }

    public void setTlsContext(TlsContextFactory tlsContextFactory)
    {
        this.tlsContextFactory = tlsContextFactory;
    }

    public void setClientId(final String clientId)
    {
        this.clientId = clientId;
    }

    public void setClientSecret(final String clientSecret)
    {
        this.clientSecret = clientSecret;
    }

    public String getClientSecret()
    {
        return clientSecret;
    }

    public String getClientId()
    {
        return clientId;
    }
    
    protected String getAccessToken(final ConfigOAuthContext context, final String resourceOwnerId)
    {
        final String accessToken;
        context.getContextForResourceOwner(resourceOwnerId).getRefreshUserOAuthContextLock().lock();
        try
        {
            accessToken = context.getContextForResourceOwner(resourceOwnerId).getAccessToken();
        }
        finally
        {
            context.getContextForResourceOwner(resourceOwnerId).getRefreshUserOAuthContextLock().unlock();
        }
        return accessToken;
    }
}
