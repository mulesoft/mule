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

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.devkit.ProcessAdapter;
import org.mule.api.devkit.ProcessTemplate;
import org.mule.api.devkit.capability.Capabilities;
import org.mule.api.devkit.capability.ModuleCapability;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.store.ObjectStore;
import org.mule.common.security.oauth.OAuthState;
import org.mule.common.security.oauth.exception.UnableToAcquireAccessTokenException;
import org.mule.config.i18n.CoreMessages;
import org.mule.security.oauth.callback.DefaultHttpCallbackAdapter;
import org.mule.security.oauth.callback.RestoreAccessTokenCallback;
import org.mule.security.oauth.callback.SaveAccessTokenCallback;
import org.mule.security.oauth.process.OAuthProcessTemplate;
import org.mule.security.oauth.util.DefaultOAuthResponseParser;
import org.mule.security.oauth.util.HttpUtil;
import org.mule.security.oauth.util.HttpUtilImpl;
import org.mule.security.oauth.util.OAuthResponseParser;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Map;

import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.slf4j.Logger;

public abstract class BaseOAuth2Manager<C extends OAuth2Adapter> extends DefaultHttpCallbackAdapter
    implements MuleContextAware, Initialisable, Capabilities, Startable, Stoppable, Disposable,
    OAuth2Manager<OAuth2Adapter>, ProcessAdapter<OAuth2Adapter>
{

    private OAuth2Adapter defaultUnauthorizedConnector;
    private String applicationName;
    private String scope;

    /**
     * muleContext
     */
    protected MuleContext muleContext;

    /**
     * Flow Construct
     */
    protected FlowConstruct flowConstruct;
    private ObjectStore<OAuthState> accessTokenObjectStore;

    /**
     * Access Token Pool Factory
     */
    private KeyedPoolableObjectFactory accessTokenPoolFactory;

    /**
     * Access Token Pool
     */
    private GenericKeyedObjectPool accessTokenPool;

    private HttpUtil httpUtil;
    private OAuthResponseParser oauthResponseParser;

    /**
     * @return the logger to be used when logging messages.
     */
    protected abstract Logger getLogger();

    /**
     * Creates a concrete instance of the OAuth2Adapter that corresponds with this
     * OAuthManager
     * 
     * @return an instance of {@link org.mule.security.oauth.OAuth2Adapter}
     */
    protected abstract OAuth2Adapter instantiateAdapter();

    /**
     * Returns the concrete instance of
     * {@link org.apache.commons.pool.KeyedPoolableObjectFactory} that's going to be
     * in charge of creating the objects in the pool
     * 
     * @param oauthManager the OAuthManager that will manage the created objects
     * @param objectStore an instance of {@link org.mule.api.store.ObjectStore} that
     *            will be responsible for storing instances of
     *            {@link org.mule.common.security.oauth.OAuthState}
     * @return an instance of
     *         {@link org.apache.commons.pool.KeyedPoolableObjectFactory}
     */
    protected abstract KeyedPoolableObjectFactory createPoolFactory(OAuth2Manager<OAuth2Adapter> oauthManager,
                                                                    ObjectStore<OAuthState> objectStore);

    /**
     * Populates the adapter with custom properties not accessible from the base
     * interface.
     * 
     * @param adapter an instance of {@link org.mule.security.oauth.OAuth2Adapter}
     */
    protected abstract void setCustomProperties(OAuth2Adapter adapter);

    /**
     * Extracts any custom parameters from the OAuth response and sets them
     * accordingly on the adapter
     * 
     * @param adapter the adapter on which the custom parameters will be set on
     * @param response the response obatined from the OAuth provider
     */
    protected abstract void fetchCallbackParameters(OAuth2Adapter adapter, String response);

    @Override
    public final void initialise() throws InitialisationException
    {
        super.initialise();
        GenericKeyedObjectPool.Config config = new GenericKeyedObjectPool.Config();
        config.testOnBorrow = true;
        if (accessTokenObjectStore == null)
        {
            accessTokenObjectStore = muleContext.getRegistry().lookupObject(
                MuleProperties.DEFAULT_USER_OBJECT_STORE_NAME);
            if (accessTokenObjectStore == null)
            {
                throw new InitialisationException(
                    CoreMessages.createStaticMessage("There is no default user object store on this Mule instance."),
                    this);
            }
        }
        accessTokenPoolFactory = this.createPoolFactory(this, this.accessTokenObjectStore);
        accessTokenPool = new GenericKeyedObjectPool(accessTokenPoolFactory, config);
        defaultUnauthorizedConnector = this.instantiateAdapter();

        if (defaultUnauthorizedConnector instanceof Initialisable)
        {
            ((Initialisable) defaultUnauthorizedConnector).initialise();
        }

        if (this.httpUtil == null)
        {
            this.httpUtil = new HttpUtilImpl();
        }

        if (this.oauthResponseParser == null)
        {
            this.oauthResponseParser = new DefaultOAuthResponseParser();
        }
    }

    /**
     * if {@link org.mule.api.lifecycle.Startable}, then
     * {@link org.mule.security.oauth.BaseOAuth2Manager.defaultUnauthorizedConnector}
     * is started
     */
    @Override
    public final void start() throws MuleException
    {
        if (defaultUnauthorizedConnector instanceof Startable)
        {
            ((Startable) defaultUnauthorizedConnector).start();
        }
    }

    /**
     * if {@link org.mule.api.lifecycle.Stoppable}, then
     * {@link org.mule.security.oauth.BaseOAuth2Manager.defaultUnauthorizedConnector}
     * is stopped
     */
    @Override
    public final void stop() throws MuleException
    {
        if (defaultUnauthorizedConnector instanceof Stoppable)
        {
            ((Stoppable) defaultUnauthorizedConnector).stop();
        }
    }

    /**
     * if {@link org.mule.api.lifecycle.Disposable}, then
     * {@link org.mule.security.oauth.BaseOAuth2Manager.defaultUnauthorizedConnector}
     * is disposed
     */
    @Override
    public final void dispose()
    {
        if (defaultUnauthorizedConnector instanceof Disposable)
        {
            ((Disposable) defaultUnauthorizedConnector).dispose();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final OAuth2Adapter createAdapter(String verifier) throws Exception
    {
        OAuth2Adapter connector = this.instantiateAdapter();
        connector.setOauthVerifier(verifier);
        connector.setAuthorizationUrl(getDefaultUnauthorizedConnector().getAuthorizationUrl());
        connector.setAccessTokenUrl(getDefaultUnauthorizedConnector().getAccessTokenUrl());
        connector.setConsumerKey(this.getDefaultUnauthorizedConnector().getConsumerKey());
        connector.setConsumerSecret(this.getDefaultUnauthorizedConnector().getConsumerSecret());

        this.setCustomProperties(connector);

        if (connector instanceof MuleContextAware)
        {
            ((MuleContextAware) connector).setMuleContext(muleContext);
        }
        if (connector instanceof Initialisable)
        {
            ((Initialisable) connector).initialise();
        }
        if (connector instanceof Startable)
        {
            ((Startable) connector).start();
        }
        return connector;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final OAuth2Adapter acquireAccessToken(String userId) throws Exception
    {
        if (getLogger().isDebugEnabled())
        {
            getLogger().debug(
                String.format("Pool Statistics before acquiring [key %s] [active=%d] [idle=%d]", userId,
                    accessTokenPool.getNumActive(userId), accessTokenPool.getNumIdle(userId)));
        }

        OAuth2Adapter object = ((OAuth2Adapter) accessTokenPool.borrowObject(userId));

        if (getLogger().isDebugEnabled())
        {
            getLogger().debug(
                String.format("Pool Statistics after acquiring [key %s] [active=%d] [idle=%d]", userId,
                    accessTokenPool.getNumActive(userId), accessTokenPool.getNumIdle(userId)));
        }
        return object;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void releaseAccessToken(String userId, OAuth2Adapter connector) throws Exception
    {
        if (getLogger().isDebugEnabled())
        {
            getLogger().debug(
                String.format("Pool Statistics before releasing [key %s] [active=%d] [idle=%d]", userId,
                    accessTokenPool.getNumActive(userId), accessTokenPool.getNumIdle(userId)));
        }

        accessTokenPool.returnObject(userId, connector);

        if (getLogger().isDebugEnabled())
        {
            getLogger().debug(
                String.format("Pool Statistics after releasing [key %s] [active=%d] [idle=%d]", userId,
                    accessTokenPool.getNumActive(userId), accessTokenPool.getNumIdle(userId)));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void destroyAccessToken(String userId, OAuth2Adapter connector) throws Exception
    {
        if (getLogger().isDebugEnabled())
        {
            getLogger().debug(
                String.format("Pool Statistics before destroying [key %s] [active=%d] [idle=%d]", userId,
                    accessTokenPool.getNumActive(userId), accessTokenPool.getNumIdle(userId)));
        }

        accessTokenPool.invalidateObject(userId, connector);

        if (getLogger().isDebugEnabled())
        {
            getLogger().debug(
                String.format("Pool Statistics after destroying [key %s] [active=%d] [idle=%d]", userId,
                    accessTokenPool.getNumActive(userId), accessTokenPool.getNumIdle(userId)));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String buildAuthorizeUrl(Map<String, String> extraParameters,
                                          String authorizationUrl,
                                          String redirectUri)
    {
        StringBuilder urlBuilder = new StringBuilder();
        if (authorizationUrl != null)
        {
            urlBuilder.append(authorizationUrl);
        }
        else
        {
            urlBuilder.append(this.getDefaultUnauthorizedConnector().getAuthorizationUrl());
        }
        urlBuilder.append("?");
        urlBuilder.append("response_type=code&");
        urlBuilder.append("client_id=");
        urlBuilder.append(this.getDefaultUnauthorizedConnector().getConsumerKey());
        urlBuilder.append("&redirect_uri=");
        urlBuilder.append(redirectUri);
        for (String parameter : extraParameters.keySet())
        {
            urlBuilder.append("&");
            urlBuilder.append(parameter);
            urlBuilder.append("=");
            urlBuilder.append(extraParameters.get(parameter));
        }

        if (getLogger().isDebugEnabled())
        {
            getLogger().debug(("Authorization URL has been generated as follows: " + urlBuilder));
        }

        return urlBuilder.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean restoreAccessToken(OAuth2Adapter adapter)
    {
        RestoreAccessTokenCallback callback = adapter.getOauthRestoreAccessToken();
        if (callback != null)
        {
            if (getLogger().isDebugEnabled())
            {
                getLogger().debug("Attempting to restore access token...");
            }

            try
            {
                callback.restoreAccessToken();
                String accessToken = callback.getAccessToken();
                adapter.setAccessToken(accessToken);

                if (getLogger().isDebugEnabled())
                {
                    getLogger().debug(
                        String.format(
                            "Access token and secret has been restored successfully [accessToken = %s]",
                            accessToken));
                }
                return true;
            }
            catch (Exception e)
            {
                getLogger().error("Cannot restore access token, an unexpected error occurred", e);
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void fetchAccessToken(OAuth2Adapter adapter, String redirectUri)
        throws UnableToAcquireAccessTokenException
    {
        StringBuilder builder = new StringBuilder();
        try
        {
            builder.append("code=");
            builder.append(URLEncoder.encode(adapter.getOauthVerifier(), "UTF-8"));
            builder.append("&client_id=");
            builder.append(URLEncoder.encode(adapter.getConsumerKey(), "UTF-8"));
            builder.append("&client_secret=");
            builder.append(URLEncoder.encode(adapter.getConsumerSecret(), "UTF-8"));
            builder.append("&grant_type=");
            builder.append(URLEncoder.encode("authorization_code", "UTF-8"));
            builder.append("&redirect_uri=");
            builder.append(URLEncoder.encode(redirectUri, "UTF-8"));
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
        this.fetchAndExtract(adapter, builder.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void refreshAccessToken(OAuth2Adapter adapter) throws UnableToAcquireAccessTokenException
    {
        if (getLogger().isDebugEnabled())
        {
            getLogger().debug("Trying to refresh access token...");
        }
        if (adapter.getRefreshToken() == null)
        {
            throw new IllegalStateException("Cannot refresh access token since refresh token is null");
        }

        StringBuilder builder = new StringBuilder();
        builder.append("grant_type=refresh_token");
        builder.append("&client_id=");
        builder.append(adapter.getConsumerKey());
        builder.append("&client_secret=");
        builder.append(adapter.getConsumerSecret());
        builder.append("&refresh_token=");
        builder.append(adapter.getRefreshToken());

        adapter.setAccessToken(null);
        this.fetchAndExtract(adapter, builder.toString());
    }

    private void fetchAndExtract(OAuth2Adapter adapter, String requestBody)
        throws UnableToAcquireAccessTokenException
    {
        this.restoreAccessToken(adapter);

        if (adapter.getAccessToken() != null)
        {
            return;
        }

        if (getLogger().isDebugEnabled())
        {
            getLogger().debug("Retrieving access token...");
        }

        String accessTokenUrl = adapter.getAccessTokenUrl() != null
                                                                   ? adapter.getAccessTokenUrl()
                                                                   : this.getDefaultUnauthorizedConnector()
                                                                       .getAccessTokenUrl();

        String response = this.httpUtil.post(accessTokenUrl, requestBody);

        if (getLogger().isDebugEnabled())
        {
            getLogger().debug(String.format("Received response [%s]", response));
        }

        adapter.setAccessToken(this.oauthResponseParser.extractAccessCode(adapter.getAccessCodePattern(),
            response));
        if (getLogger().isDebugEnabled())
        {
            getLogger().debug(
                String.format("Access token retrieved successfully [accessToken = %s]",
                    adapter.getAccessToken()));
        }

        this.saveAccessToken(adapter);

        if (getLogger().isDebugEnabled())
        {
            getLogger().debug(
                String.format("Attempting to extract expiration time using [expirationPattern = %s]",
                    adapter.getExpirationTimePattern().pattern()));
        }

        Date expiration = this.oauthResponseParser.extractExpirationTime(adapter.getExpirationTimePattern(),
            response);
        if (expiration != null)
        {

            adapter.setExpiration(expiration);

            if (getLogger().isDebugEnabled())
            {
                getLogger().debug(
                    String.format("Token expiration extracted successfully [expiration = %s]", expiration));
            }
        }
        else
        {
            if (getLogger().isDebugEnabled())
            {
                getLogger().debug(
                    String.format("Token expiration could not be extracted from [response = %s]", response));
            }
        }

        if (getLogger().isDebugEnabled())
        {
            getLogger().debug(
                "Attempting to extract refresh token time using [refreshTokenPattern = \"refresh_token\":\"([^&]+?)\"]");
        }

        String refreshToken = this.oauthResponseParser.extractRefreshToken(adapter.getRefreshTokenPattern(),
            response);

        if (refreshToken != null)
        {

            adapter.setRefreshToken(refreshToken);

            if (getLogger().isDebugEnabled())
            {
                getLogger().debug(
                    String.format("Refresh token extracted successfully [refresh token = %s]", refreshToken));
            }
        }
        else
        {
            if (getLogger().isDebugEnabled())
            {
                getLogger().debug(
                    String.format("Refresh token could not be extracted from [response = %s]", response));
            }
        }

        this.fetchCallbackParameters(adapter, response);
        adapter.postAuth();
    }

    private void saveAccessToken(OAuth2Adapter adapter)
    {
        SaveAccessTokenCallback saveCallback = adapter.getOauthSaveAccessToken();

        if (saveCallback != null)
        {
            try
            {
                if (getLogger().isDebugEnabled())
                {
                    getLogger().debug(
                        String.format("Attempting to save access token...[accessToken = %s]",
                            adapter.getAccessToken()));
                }
                saveCallback.saveAccessToken(adapter.getAccessToken(), null);
            }
            catch (Exception e)
            {
                getLogger().error("Cannot save access token, an unexpected error occurred", e);
            }
        }
    }

    /**
     * Returns true if this module implements such capability
     */
    public final boolean isCapableOf(ModuleCapability capability)
    {
        if (capability == ModuleCapability.LIFECYCLE_CAPABLE)
        {
            return true;
        }
        if (capability == ModuleCapability.OAUTH2_CAPABLE)
        {
            return true;
        }
        if (capability == ModuleCapability.OAUTH_ACCESS_TOKEN_MANAGEMENT_CAPABLE)
        {
            return true;
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> ProcessTemplate<T, OAuth2Adapter> getProcessTemplate()
    {
        return (ProcessTemplate<T, OAuth2Adapter>) new OAuthProcessTemplate(this);
    }

    /**
     * Retrieves defaultUnauthorizedConnector
     */
    @Override
    public OAuth2Adapter getDefaultUnauthorizedConnector()
    {
        return this.defaultUnauthorizedConnector;
    }

    /**
     * Sets applicationName
     * 
     * @param value Value to set
     */
    public void setApplicationName(String value)
    {
        this.applicationName = value;
    }

    /**
     * Retrieves applicationName
     */
    public String getApplicationName()
    {
        return this.applicationName;
    }

    /**
     * Sets scope
     * 
     * @param value Value to set
     */
    public void setScope(String value)
    {
        this.scope = value;
    }

    /**
     * Retrieves scope
     */
    public String getScope()
    {
        return this.scope;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final MuleContext getMuleContext()
    {
        return this.muleContext;
    }

    public final void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
        if (defaultUnauthorizedConnector instanceof MuleContextAware)
        {
            ((MuleContextAware) defaultUnauthorizedConnector).setMuleContext(muleContext);
        }
    }

    /**
     * Retrieves flowConstruct
     */
    public FlowConstruct getFlowConstruct()
    {
        return this.flowConstruct;
    }

    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
        if (defaultUnauthorizedConnector instanceof FlowConstructAware)
        {
            ((FlowConstructAware) defaultUnauthorizedConnector).setFlowConstruct(flowConstruct);
        }
    }

    /**
     * Retrieves accessTokenObjectStore
     */
    public ObjectStore<OAuthState> getAccessTokenObjectStore()
    {
        return this.accessTokenObjectStore;
    }

    /**
     * Sets accessTokenObjectStore
     * 
     * @param value Value to set
     */
    public void setAccessTokenObjectStore(ObjectStore<OAuthState> value)
    {
        this.accessTokenObjectStore = value;
    }

    /**
     * {@inheritDoc}
     */
    public KeyedPoolableObjectFactory getAccessTokenPoolFactory()
    {
        return this.accessTokenPoolFactory;
    }

    public void setHttpUtil(HttpUtil httpUtil)
    {
        this.httpUtil = httpUtil;
    }

    public void setOauthResponseParser(OAuthResponseParser oauthResponseParser)
    {
        this.oauthResponseParser = oauthResponseParser;
    }
}
