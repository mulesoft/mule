/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.security.oauth;

import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.common.security.oauth.OAuthState;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.KeyedPoolableObjectFactory;

public abstract class BaseOAuthClientFactory implements KeyedPoolableObjectFactory
{

    private static final transient Log logger = LogFactory.getLog(BaseOAuthClientFactory.class);

    private OAuthManager<OAuthAdapter> oauthManager;
    private ObjectStore<OAuthState> objectStore;

    public BaseOAuthClientFactory(OAuthManager<OAuthAdapter> oauthManager, ObjectStore<OAuthState> objectStore)
    {
        this.oauthManager = oauthManager;
        this.objectStore = objectStore;
    }

    protected abstract Class<? extends OAuthAdapter> getAdapterClass();

    protected abstract void setCustomAdapterProperties(OAuthAdapter adapter, OAuthState state);

    protected abstract void setCustomStateProperties(OAuthAdapter adapter, OAuthState state);

    @Override
    public final Object makeObject(Object key) throws Exception
    {
        if (!(key instanceof String))
        {
            throw new IllegalArgumentException("Invalid key type");
        }

        OAuthState state = null;
        if (!this.objectStore.contains(((String) key)))
        {
            throw new RuntimeException(
                (("There is no access token stored under the key " + ((String) key)) + ". You need to call the <authorize> message processor. The key will be given to you via a flow variable after the OAuth dance is completed. You can extract it using flowVars['tokenId']."));
        }

        state = this.objectStore.retrieve((String) key);

        OAuthAdapter connector = this.getAdapterClass()
            .getConstructor(OAuthManager.class)
            .newInstance(this.oauthManager);
        
        connector.setConsumerKey(oauthManager.getConsumerKey());
        connector.setConsumerSecret(oauthManager.getConsumerSecret());
        connector.setAccessToken(state.getAccessToken());
        connector.setAuthorizationUrl(state.getAuthorizationUrl());
        connector.setAccessTokenUrl(state.getAccessTokenUrl());
        connector.setRefreshToken(state.getRefreshToken());

        this.setCustomAdapterProperties(connector, state);

        if (connector instanceof Initialisable)
        {
            ((Initialisable) connector).initialise();
        }
        if (connector instanceof MuleContextAware)
        {
            ((MuleContextAware) connector).setMuleContext(oauthManager.getMuleContext());
        }
        if (connector instanceof Startable)
        {
            ((Startable) connector).start();
        }

        connector.postAuth();
        return connector;
    }

    public final void destroyObject(Object key, Object obj) throws Exception
    {
        if (!(key instanceof String))
        {
            throw new IllegalArgumentException("Invalid key type");
        }

        if (!this.getAdapterClass().isInstance(obj))
        {
            throw new IllegalArgumentException("Invalid connector type");
        }

        if (obj instanceof Stoppable)
        {
            ((Stoppable) obj).stop();
        }

        if (obj instanceof Disposable)
        {
            ((Disposable) obj).dispose();
        }
    }

    public final boolean validateObject(Object key, Object obj)
    {
        if (!(key instanceof String))
        {
            throw new IllegalArgumentException("Invalid key type");
        }

        String k = (String) key;

        if (!this.getAdapterClass().isInstance(obj))
        {
            throw new IllegalArgumentException("Invalid connector type");
        }

        OAuthAdapter connector = (OAuthAdapter) obj;

        OAuthState state = null;
        try
        {
            if (!this.objectStore.contains(k))
            {
                return false;
            }

            state = this.objectStore.retrieve(k);

            if (connector.getAccessToken() == null)
            {
                return false;
            }

            if (!connector.getAccessToken().equals(state.getAccessToken()))
            {
                return false;
            }
            if (connector.getRefreshToken() == null && state.getRefreshToken() != null)
            {
                return false;
            }

            if (connector.getRefreshToken() != null
                && !connector.getRefreshToken().equals(state.getRefreshToken()))
            {
                return false;
            }
        }
        catch (ObjectStoreException e)
        {
            logger.warn("Could not validate object due to object store exception", e);
            return false;
        }
        return true;
    }

    public void activateObject(Object key, Object obj) throws Exception
    {
    }

    public final void passivateObject(Object key, Object obj) throws Exception
    {
        if (!(key instanceof String))
        {
            throw new IllegalArgumentException("Invalid key type");
        }

        String k = (String) key;

        if (!this.getAdapterClass().isInstance(obj))
        {
            throw new IllegalArgumentException("Invalid connector type");
        }

        OAuthAdapter connector = (OAuthAdapter) obj;

        OAuthState state = null;

        if (this.objectStore.contains(((String) key)))
        {
            state = this.objectStore.retrieve(k);
            this.objectStore.remove(k);
        }

        if (state == null)
        {
            state = new OAuthState();
        }

        state.setAccessToken(connector.getAccessToken());
        state.setAccessTokenUrl(connector.getAccessTokenUrl());
        state.setAuthorizationUrl(connector.getAuthorizationUrl());
        state.setRefreshToken(connector.getRefreshToken());

        this.setCustomStateProperties(connector, state);

        this.objectStore.store(k, state);
    }

}
