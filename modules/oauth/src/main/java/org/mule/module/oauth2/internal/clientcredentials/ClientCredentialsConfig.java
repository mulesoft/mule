/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.clientcredentials;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.store.ListableObjectStore;
import org.mule.module.oauth2.internal.ClientApplicationCredentials;
import org.mule.module.oauth2.internal.OAuthGrantTypeConfig;

public class ClientCredentialsConfig implements Initialisable, Startable, MuleContextAware, ClientApplicationCredentials, OAuthGrantTypeConfig
{

    private String name;
    private String clientId;
    private String clientSecret;
    private ClientCredentialsTokenRequestHandler tokenRequestHandler;
    private MuleContext muleContext;
    private ObjectStoreClientCredentialsStore clientCredentialsStore;
    private ClientCredentialsStoreContextMelAdapter melFunctionaClientCredentialsStore;

    public void setName(final String name)
    {
        this.name = name;
    }

    public void setClientId(final String clientId)
    {
        this.clientId = clientId;
    }

    public void setClientSecret(final String clientSecret)
    {
        this.clientSecret = clientSecret;
    }

    public void setTokenRequestHandler(final ClientCredentialsTokenRequestHandler tokenRequestHandler)
    {
        this.tokenRequestHandler = tokenRequestHandler;
    }

    @Override
    public void start() throws MuleException
    {
        tokenRequestHandler.refreshAccessToken();
    }

    public String getClientSecret()
    {
        return clientSecret;
    }

    public String getClientId()
    {
        return clientId;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        clientCredentialsStore = new ObjectStoreClientCredentialsStore((ListableObjectStore) muleContext.getObjectStoreManager().getObjectStore("client-credentials-store-" + this.name));
        melFunctionaClientCredentialsStore = new ClientCredentialsStoreContextMelAdapter(clientCredentialsStore);
        tokenRequestHandler.setClientCredentialsStore(clientCredentialsStore);
        tokenRequestHandler.setClientApplicationCredentials(this);
    }

    @Override
    public void setMuleContext(final MuleContext context)
    {
        this.muleContext = context;
    }

    public ObjectStoreClientCredentialsStore getClientCredentialsStore()
    {
        return clientCredentialsStore;
    }

    public String getRefreshTokenWhen()
    {
        return tokenRequestHandler.getRefreshTokenWhen();
    }

    public void refreshAccessToken() throws MuleException
    {
        tokenRequestHandler.refreshAccessToken();
    }

    @Override
    public Object processOauthContextFunctionACall(Object[] params)
    {
        if (params.length > 0)
        {
            throw new IllegalArgumentException("Client credentials oauthContext function accepts only the config name parameter");
        }
        return melFunctionaClientCredentialsStore;
    }
}
