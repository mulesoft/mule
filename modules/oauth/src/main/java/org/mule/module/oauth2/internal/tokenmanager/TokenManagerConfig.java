/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.tokenmanager;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.api.store.ListableObjectStore;
import org.mule.module.oauth2.internal.authorizationcode.state.ConfigOAuthContext;
import org.mule.module.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext;
import org.mule.util.store.MuleObjectStoreManager;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Token manager stores all the OAuth State (access token, refresh token).
 *
 * It can be referenced to access the state inside a flow for custom processing of oauth dance content.
 */
public class TokenManagerConfig implements Initialisable, MuleContextAware
{

    public static AtomicInteger defaultTokenManagerConfigIndex = new AtomicInteger(0);
    private String name;
    private ListableObjectStore objectStore;
    private ConfigOAuthContext configOAuthContext;
    private MuleContext muleContext;
    private boolean initialised;


    public void setObjectStore(ListableObjectStore objectStore)
    {
        this.objectStore = objectStore;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public synchronized void initialise() throws InitialisationException
    {
        if (initialised)
        {
            return;
        }
        if (objectStore == null)
        {
            objectStore = (ListableObjectStore) ((MuleObjectStoreManager) muleContext.getObjectStoreManager()).getUserObjectStore("token-manager-store-" + this.name, true);
        }
        configOAuthContext = new ConfigOAuthContext(muleContext.getLockFactory(), objectStore, name);
        initialised = true;
    }

    public static TokenManagerConfig createDefault(final MuleContext context) throws InitialisationException
    {
        final TokenManagerConfig tokenManagerConfig = new TokenManagerConfig();
        final String tokenManagerConfigName = "default-token-manager-config-" + defaultTokenManagerConfigIndex.getAndIncrement();
        tokenManagerConfig.setName(tokenManagerConfigName);
        try
        {
            context.getRegistry().registerObject(tokenManagerConfigName, tokenManagerConfig);
        }
        catch (RegistrationException e)
        {
            throw new InitialisationException(e, tokenManagerConfig);
        }
        return tokenManagerConfig;
    }

    public ConfigOAuthContext getConfigOAuthContext()
    {
        return configOAuthContext;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    /**
     * Provides support for the oauthContext MEL function for this configuration
     *
     * @param params function parameters without the config name parameter
     * @return the result of the function call
     */
    public Object processOauthContextFunctionACall(Object[] params)
    {
        if (params.length > 1)
        {
            throw new IllegalArgumentException(String.format("oauthContext for config type %s does not accepts more than two arguments", "authorization-code"));
        }
        String resourceOwnerId = ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;
        if (params.length == 1)
        {
            resourceOwnerId = (String) params[0];
        }
        return getConfigOAuthContext().getContextForResourceOwner(resourceOwnerId);
    }
}
