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

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseOAuthClientFactory implements KeyedPoolableObjectFactory
{

    private static transient Logger logger = LoggerFactory.getLogger(BaseOAuthClientFactory.class);
    private static transient Map<String, PropertyDescriptor> oauthStateProperties = new HashMap<String, PropertyDescriptor>();

    static
    {
        try
        {
            for (PropertyDescriptor pd : Introspector.getBeanInfo(OAuthState.class, Object.class)
                .getPropertyDescriptors())
            {
                oauthStateProperties.put(pd.getName(), pd);
            }
        }
        catch (IntrospectionException e)
        {
            throw new RuntimeException(
                "Error initializing OAuthClientFactory. Could not introspect OAuthState", e);
        }
    }

    private OAuth2Manager<OAuth2Adapter> oauthManager;
    private ObjectStore<Serializable> objectStore;

    public BaseOAuthClientFactory(OAuth2Manager<OAuth2Adapter> oauthManager,
                                  ObjectStore<Serializable> objectStore)
    {
        this.oauthManager = oauthManager;
        this.objectStore = objectStore;
    }

    /**
     * Returns the class of the concrete implementation of
     * {@link org.mule.security.oauth.OAuth2Adapter} that this factory is supposed to
     * generate
     */
    protected abstract Class<? extends OAuth2Adapter> getAdapterClass();

    /**
     * Implementors can levarage this method to move custom property values from the
     * state to the adapter You can leave this method blank if not needed in your
     * case.
     * 
     * @param adapter a {@link org.mule.security.oauth.OAuth2Adapter} which supports
     *            custom properties
     * @param state a {@link org.mule.security.oauth.OAuth2Adapter} which is carrying
     *            custom properties.
     */
    protected abstract void setCustomAdapterProperties(OAuth2Adapter adapter, OAuthState state);

    /**
     * Implementos can leverage this method to move custom property values from the
     * adapter to the state. You can leave this method blank if not needed in your
     * case.
     * 
     * @param adapter a {@link org.mule.security.oauth.OAuth2Adapter} which is
     *            carrying custom properties
     * @param state a {@link org.mule.security.oauth.OAuth2Adapter}
     */
    protected abstract void setCustomStateProperties(OAuth2Adapter adapter, OAuthState state);

    /**
     * This method creates an instance of
     * {@link org.mule.security.oauth.OAuth2Adapter} which concrete type depends on
     * the return of
     * {@link org.mule.security.oauth.BaseOAuthClientFactory.getAdapterClass} The
     * adapter is fully initialized and interfaces such as
     * {@link org.mule.api.lifecycle.Initialisable},
     * {@link org.mule.api.context.MuleContextAware} and
     * {@link org.mule.api.lifecycle.Startable} are respected by invoking the
     * corresponding methods in case the adapter implements them. Finally, the
     * {@link org.mule.security.oauth.OAuth2Connector.postAuth()} method is invoked.
     * 
     * @param the key of the object at the object store
     * @throws IllegalArgumentException if key is not a String
     */
    @Override
    public final Object makeObject(Object key) throws Exception
    {
        if (!(key instanceof String))
        {
            throw new IllegalArgumentException("Invalid key type");
        }

        if (!this.objectStore.contains(((String) key)))
        {
            throw new RuntimeException(
                (("There is no access token stored under the key " + ((String) key)) + ". You need to call the <authorize> message processor. The key will be given to you via a flow variable after the OAuth dance is completed. You can extract it using flowVars['tokenId']."));
        }

        OAuthState state = this.retrieveOAuthState((String) key, true);

        OAuth2Adapter connector = this.getAdapterClass()
            .getConstructor(OAuth2Manager.class)
            .newInstance(this.oauthManager);

        connector.setConsumerKey(oauthManager.getDefaultUnauthorizedConnector().getConsumerKey());
        connector.setConsumerSecret(oauthManager.getDefaultUnauthorizedConnector().getConsumerSecret());
        connector.setAccessToken(state.getAccessToken());
        connector.setAuthorizationUrl(state.getAuthorizationUrl());
        connector.setAccessTokenUrl(state.getAccessTokenUrl());
        connector.setRefreshToken(state.getRefreshToken());

        this.setCustomAdapterProperties(connector, state);

        if (connector instanceof MuleContextAware)
        {
            ((MuleContextAware) connector).setMuleContext(oauthManager.getMuleContext());
        }
        if (connector instanceof Initialisable)
        {
            ((Initialisable) connector).initialise();
        }
        if (connector instanceof Startable)
        {
            ((Startable) connector).start();
        }

        connector.postAuth();
        return connector;
    }

    /**
     * This method is to provide backwards compatibility with connectors generated
     * using devkit 3.4.0 or lower. Those connectors used one custom OAuthState class
     * per connector which disabled the possibility of token sharing. The
     * {@link org.mule.common.security.oauth.OAuthState} solves that problem but
     * generates a migration issues with applications using such a connector and
     * wishing to upgrade. This method retrieves a value from the ObjectStore and if
     * it's not a generic org.mule.common.security.oauth.OAuthState instance it
     * translates it to one, generating an ongoing migration process. If
     * <code>replace</code> is true, then the value in the ObjectStore is replaced.
     * Once Mule 3.4.0 reaches end of life status, this method can be replaced by
     * simple object store lookup
     * 
     * @param key the object store key
     * @param replace if true and if the obtained value is not an instance of
     *            {@link org.mule.common.security.oauth.OAuthState}, then the value
     *            is replaced in the object store by the transformed instance
     */
    private synchronized OAuthState retrieveOAuthState(String key, boolean replace)
    {
        Object state = null;
        try
        {
            state = this.objectStore.retrieve(key);
        }
        catch (ObjectStoreException e)
        {
            throw new RuntimeException("Error retrievin value from object store with key " + key, e);
        }

        if (state != null && !(state instanceof OAuthState))
        {

            OAuthState newState = new OAuthState();

            try
            {
                for (PropertyDescriptor beanProperty : Introspector.getBeanInfo(state.getClass(),
                    Object.class).getPropertyDescriptors())
                {
                    Object value = beanProperty.getReadMethod().invoke(state, (Object[]) null);
                    if (value != null)
                    {
                        PropertyDescriptor stateProperty = oauthStateProperties.get(beanProperty.getName());
                        if (stateProperty != null)
                        {
                            stateProperty.getWriteMethod().invoke(newState, value);
                        }
                        else
                        {
                            newState.setCustomProperty(beanProperty.getName(), value.toString());
                        }
                    }
                }
            }
            catch (IllegalAccessException e)
            {
                throw new RuntimeException("Error accessing value through reflection", e);
            }
            catch (IntrospectionException e)
            {
                throw new RuntimeException("Error introspecting object of class "
                                           + state.getClass().getCanonicalName(), e);
            }
            catch (IllegalArgumentException e)
            {
                throw new RuntimeException("Error setting value through reflection", e);
            }
            catch (InvocationTargetException e)
            {
                throw new RuntimeException("Object threw exception while setting value by reflection", e);
            }

            state = newState;

            if (replace)
            {
                try
                {
                    this.objectStore.remove(key);
                    this.objectStore.store(key, newState);
                }
                catch (ObjectStoreException e)
                {
                    throw new RuntimeException("ObjectStore threw exception while replacing instance", e);
                }
            }
        }

        return (OAuthState) state;

    }

    /**
     * If obj implements {@link org.mule.api.lifecycle.Stoppable} or
     * {@link org.mule.api.lifecycle.Disposable}, the object is destroyed by invoking
     * the corresponding methods
     * 
     * @param key the key of the object at the object store
     * @param obj an instance of {@link org.mule.security.oauth.OAuth2Adapter}
     * @throws IllegalArgumetException if key is not a string or if obj is not an
     *             instance of the type returned by {@link
     *             org.mule.security.oauth.BaseOAuthClientFactory.getAdapterClass()}
     */
    @Override
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

    /**
     * Validates the object by checking that it exists at the object store and that
     * the state of the given object is consisten with the persisted state
     * 
     * @param key the key of the object at the object store
     * @param obj an instance of {@link org.mule.security.oauth.OAuth2Adapter}
     * @throws IllegalArgumetException if key is not a string or if obj is not an
     *             instance of the type returned by {@link
     *             org.mule.security.oauth.BaseOAuthClientFactory.getAdapterClass()}
     */
    @Override
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

        OAuth2Adapter connector = (OAuth2Adapter) obj;

        try
        {
            if (!this.objectStore.contains(k))
            {
                return false;
            }

            OAuthState state = this.retrieveOAuthState(k, true);

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

    /**
     * This default implementation does nothing
     */
    @Override
    public void activateObject(Object key, Object obj) throws Exception
    {
    }

    /**
     * Passivates the object by updating the state of the persisted object with the
     * one of the given one. If the object doesn't exist in the object store then it
     * is created
     * 
     * @param key the key of the object at the object store
     * @param obj an instance of {@link org.mule.security.oauth.OAuth2Adapter}
     * @throws IllegalArgumetException if key is not a string or if obj is not an
     *             instance of the type returned by {@link
     *             org.mule.security.oauth.BaseOAuthClientFactory.getAdapterClass()}
     */
    @Override
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

        OAuth2Adapter connector = (OAuth2Adapter) obj;

        OAuthState state = null;

        if (this.objectStore.contains(k))
        {
            state = this.retrieveOAuthState(k, false);
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
