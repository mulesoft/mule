/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.security.oauth;

import static org.mule.util.ClassUtils.isConsumable;

import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.NameableObject;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.devkit.ProcessTemplate;
import org.mule.api.devkit.capability.Capabilities;
import org.mule.api.devkit.capability.ModuleCapability;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.registry.RegistrationException;
import org.mule.api.store.ObjectDoesNotExistException;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.common.security.oauth.exception.NotAuthorizedException;
import org.mule.common.security.oauth.exception.UnableToAcquireAccessTokenException;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.security.oauth.callback.DefaultHttpCallbackAdapter;
import org.mule.security.oauth.process.ManagedAccessTokenProcessTemplate;
import org.mule.security.oauth.util.DefaultOAuthResponseParser;
import org.mule.security.oauth.util.HttpUtil;
import org.mule.security.oauth.util.HttpUtilImpl;
import org.mule.security.oauth.util.OAuthResponseParser;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.slf4j.Logger;

public abstract class BaseOAuth2Manager<C extends OAuth2Adapter> extends DefaultHttpCallbackAdapter
    implements MuleContextAware, Initialisable, Capabilities, Startable, Stoppable, Disposable,
    OAuth2Manager<OAuth2Adapter>, NameableObject
{

    public static final String ACCESS_TOKEN_URL = "_OAUTH_ACCESS_TOKEN_URL";
    public static final String AUTHORIZATION_URL = "_OAUTH_AUTHORIZATION_URL";

    private OAuth2Adapter defaultUnauthorizedConnector;
    private String applicationName;
    private String scope;
    private RefreshTokenManager refreshTokenManager;

    /**
     * muleContext
     */
    protected MuleContext muleContext;

    /**
     * Flow Construct
     */
    protected FlowConstruct flowConstruct;
    private ObjectStore<Serializable> accessTokenObjectStore;

    /**
     * Access Token Pool Factory
     */
    private KeyedPoolableObjectFactory<String, OAuth2Adapter> accessTokenPoolFactory;

    /**
     * Access Token Pool
     */
    private GenericKeyedObjectPool<String, OAuth2Adapter> accessTokenPool;

    private HttpUtil httpUtil;
    private OAuthResponseParser oauthResponseParser;

    private String defaultAccessTokenId;

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
    protected abstract KeyedPoolableObjectFactory<String, OAuth2Adapter> createPoolFactory(OAuth2Manager<OAuth2Adapter> oauthManager,
                                                                                           ObjectStore<Serializable> objectStore);

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

    public BaseOAuth2Manager()
    {
        this.defaultUnauthorizedConnector = this.instantiateAdapter();
    }

    @Override
    public final void initialise() throws InitialisationException
    {
        super.initialise();
        GenericKeyedObjectPool.Config config = new GenericKeyedObjectPool.Config();
        config.testOnBorrow = true;
        if (this.accessTokenObjectStore == null)
        {
            this.accessTokenObjectStore = muleContext.getRegistry().lookupObject(
                MuleProperties.DEFAULT_USER_OBJECT_STORE_NAME);
            if (this.accessTokenObjectStore == null)
            {
                throw new InitialisationException(
                    CoreMessages.createStaticMessage("There is no default user object store on this Mule instance."),
                    this);
            }
        }

        this.accessTokenPoolFactory = this.createPoolFactory(this, this.accessTokenObjectStore);
        this.accessTokenPool = new GenericKeyedObjectPool<String, OAuth2Adapter>(accessTokenPoolFactory,
            config);

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

        if (this.refreshTokenManager == null)
        {
            try
            {
                this.refreshTokenManager = this.muleContext.getRegistry().lookupObject(
                    RefreshTokenManager.class);
            }
            catch (RegistrationException e)
            {
                throw new InitialisationException(e, this);
            }
        }

    }

    /**
     * if {@link org.mule.api.lifecycle.Startable}, then
     * {@link BaseOAuth2Manager#defaultUnauthorizedConnector}
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
     * {@link BaseOAuth2Manager#defaultUnauthorizedConnector}
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
     * {@link BaseOAuth2Manager#defaultUnauthorizedConnector}
     * is disposed
     */
    @Override
    public final void dispose()
    {
        if (defaultUnauthorizedConnector instanceof Disposable)
        {
            ((Disposable) defaultUnauthorizedConnector).dispose();
        }

        try
        {
            this.accessTokenPool.close();
        }
        catch (Exception e)
        {
            this.getLogger().warn("Exception found while trying to close access token pool", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final OAuth2Adapter createAdapter(MuleEvent event, String verifier) throws Exception
    {
        OAuth2Adapter connector = this.instantiateAdapter();
        connector.setOauthVerifier(verifier);
        connector.setAuthorizationUrl((String) event.getFlowVariable(AUTHORIZATION_URL));
        connector.setAccessTokenUrl((String) event.getFlowVariable(ACCESS_TOKEN_URL));
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
    public final OAuth2Adapter acquireAccessToken(String accessTokenId) throws Exception
    {
        if (getLogger().isDebugEnabled())
        {
            getLogger().debug(
                    String.format("Pool Statistics before acquiring [key %s] [active=%d] [idle=%d]",
                                  accessTokenId, accessTokenPool.getNumActive(accessTokenId),
                                  accessTokenPool.getNumIdle(accessTokenId)));
        }

        OAuth2Adapter object = accessTokenPool.borrowObject(accessTokenId);

        if (getLogger().isDebugEnabled())
        {
            getLogger().debug(
                    String.format("Pool Statistics after acquiring [key %s] [active=%d] [idle=%d]",
                                  accessTokenId, accessTokenPool.getNumActive(accessTokenId),
                                  accessTokenPool.getNumIdle(accessTokenId)));
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

        urlBuilder.append("?")
            .append("response_type=code&")
            .append("client_id=")
            .append(this.getDefaultUnauthorizedConnector().getConsumerKey());

        try
        {
            if (!StringUtils.isBlank(this.getScope()))
            {
                urlBuilder.append("&scope=").append(URLEncoder.encode(this.getScope(), "UTF-8"));
            }

            for (Map.Entry<String, String> entry : extraParameters.entrySet())
            {
                urlBuilder.append("&")
                    .append(entry.getKey())
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }

            urlBuilder.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, "UTF-8"));
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
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
        this.fetchAndExtract(adapter, builder.toString(), null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void hasBeenAuthorized(OAuth2Adapter adapter) throws NotAuthorizedException
    {
        if (adapter.getAccessToken() == null)
        {
            throw new NotAuthorizedException(
                "This connector has not yet been authorized, please authorize by calling \"authorize\".");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void refreshAccessToken(OAuth2Adapter adapter, String accessTokenId)
        throws UnableToAcquireAccessTokenException
    {
        if (adapter.getRefreshToken() == null)
        {
            throw new IllegalStateException("Cannot refresh access token since refresh token is null");
        }

        if (getLogger().isDebugEnabled())
        {
            getLogger().debug("Trying to refresh access token...");
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
        this.fetchAndExtract(adapter, builder.toString(), accessTokenId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storeAuthorizationEvent(MuleEvent event) throws Exception
    {
        MuleMessage message = event.getMessage();

        if (message instanceof DefaultMuleMessage)
        {
            if (isConsumable(message.getPayload().getClass()))
            {
                try
                {
                    message.setPayload(message.getPayload(String.class));
                }
                catch (Exception e)
                {
                    throw new MessagingException(
                        MessageFactory.createStaticMessage(String.format(
                            "event can't be persisted because payload of class %s couldn't be consumed into a string",
                            message.getPayload().getClass().getCanonicalName())), event, e);
                }
            }
        }

        if (!(message.getPayload() instanceof Serializable))
        {
            throw new MessagingException(
                MessageFactory.createStaticMessage(String.format(
                    "In order to perform the OAuth authorization dance the mule event needs to be stored in the object store. However, the message has a payload of class %s which is not serializable.",
                    message.getPayload().getClass().getCanonicalName())), event);
        }

        String key = this.buildAuthorizationEventKey(event.getId());
        synchronized (event)
        {
            try
            {
                if (this.accessTokenObjectStore.contains(key))
                {
                    this.accessTokenObjectStore.remove(key);
                }
                this.accessTokenObjectStore.store(key, event);
            }
            catch (ObjectStoreException e)
            {
                throw new MessagingException(
                    MessageFactory.createStaticMessage("Exception was thrown when trying to store the message into object store. Please check that all message properties are serializable"),
                    event, e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MuleEvent restoreAuthorizationEvent(String eventId)
        throws ObjectStoreException, ObjectDoesNotExistException
    {
        Serializable maybeEvent = this.accessTokenObjectStore.retrieve(this.buildAuthorizationEventKey(eventId));
        if (maybeEvent instanceof MuleEvent)
        {
            MuleEvent event = (MuleEvent) maybeEvent;

            if (event instanceof ThreadSafeAccess)
            {
                ((ThreadSafeAccess) event).resetAccessControl();
            }

            return event;
        }
        else
        {
            throw new IllegalArgumentException(String.format(
                "Tried to retrieve authorization event of id %s but instead found object of class %s",
                eventId, maybeEvent.getClass().getCanonicalName()));
        }
    }

    private String buildAuthorizationEventKey(String eventId)
    {
        return String.format(OAuthProperties.AUTHORIZATION_EVENT_KEY_TEMPLATE, eventId);
    }

    private void fetchAndExtract(OAuth2Adapter adapter, String requestBody, String accessTokenId)
        throws UnableToAcquireAccessTokenException
    {
        if (adapter.getAccessToken() != null)
        {
            return;
        }

        getLogger().debug("Retrieving access token...");

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
        try
        {
            this.postAuth(adapter, accessTokenId);
        }
        catch (Exception e)
        {
            throw new UnableToAcquireAccessTokenException(
                "Adapter was successfuly retrieved but an exception was found after invoking the postAuth() method",
                e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postAuth(OAuth2Adapter adapter, String accessTokenId) throws Exception
    {
        try
        {
            adapter.postAuth();
        }
        catch (Exception e)
        {
            boolean tokenRefreshed = false;
            if (accessTokenId != null)
            {
                for (Class<? extends Exception> clazz : this.refreshAccessTokenOn())
                {
                    if (clazz.isAssignableFrom(e.getClass()))
                    {
                        Logger logger = this.getLogger();
                        if (logger.isDebugEnabled())
                        {
                            logger.debug(String.format(
                                "Tried to execute postAuth() postAuth in adapter of class %s with accessTokenId %s but token was expired. Attempting refresh",
                                adapter.getClass().getCanonicalName(), accessTokenId));
                        }
                        try
                        {
                            this.refreshTokenManager.refreshToken(adapter, accessTokenId);
                            accessTokenPoolFactory.passivateObject(accessTokenId, adapter);
                            adapter.postAuth();
                            tokenRefreshed = true;
                        }
                        catch (Exception re)
                        {
                            logger.error(String.format(
                                "Could not refresh access token %s on adapter of class %s while attempting postAuth(). Will throw the original exception",
                                accessTokenId, adapter.getClass().getCanonicalName()));
                            throw e;
                        }
                    }
                }
            }

            if (!tokenRefreshed)
            {
                throw e;
            }
        }
    }


    protected Set<Class<? extends Exception>> refreshAccessTokenOn()
    {
        return Collections.emptySet();
    }

    /**
     * Returns true if this module implements such capability
     */
    @Override
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
    public <T> ProcessTemplate<T, OAuth2Adapter> getProcessTemplate()
    {
        return new ManagedAccessTokenProcessTemplate<>(this, this.muleContext);
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

    @Override
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
    public ObjectStore<Serializable> getAccessTokenObjectStore()
    {
        return this.accessTokenObjectStore;
    }

    /**
     * Sets accessTokenObjectStore
     * 
     * @param value Value to set
     */
    public void setAccessTokenObjectStore(ObjectStore<Serializable> value)
    {
        this.accessTokenObjectStore = value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KeyedPoolableObjectFactory<String, OAuth2Adapter> getAccessTokenPoolFactory()
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

    /**
     * Sets authorizationUrl
     * 
     * @param value Value to set
     */
    public void setAuthorizationUrl(String value)
    {
        this.defaultUnauthorizedConnector.setAuthorizationUrl(value);
    }

    /**
     * Sets accessTokenUrl
     * 
     * @param value Value to set
     */
    public void setAccessTokenUrl(String value)
    {
        this.defaultUnauthorizedConnector.setAccessTokenUrl(value);
    }

    /**
     * Sets consumerKey
     * 
     * @param value Value to set
     */
    public void setConsumerKey(String value)
    {
        this.defaultUnauthorizedConnector.setConsumerKey(value);
    }

    /**
     * Sets consumerSecret
     * 
     * @param value Value to set
     */
    public void setConsumerSecret(String value)
    {
        this.defaultUnauthorizedConnector.setConsumerSecret(value);
    }

    protected void setDefaultUnauthorizedConnector(OAuth2Adapter defaultUnauthorizedConnector)
    {
        this.defaultUnauthorizedConnector = defaultUnauthorizedConnector;
    }

    public String getConsumerKey()
    {
        return this.defaultUnauthorizedConnector.getConsumerKey();
    }

    public String getConsumerSecret()
    {
        return this.defaultUnauthorizedConnector.getConsumerSecret();
    }

    @Override
    public String getName()
    {
        return this.defaultUnauthorizedConnector.getName();
    }

    @Override
    public void setName(String name)
    {
        this.defaultUnauthorizedConnector.setName(name);
    }

    @Override
    public OnNoTokenPolicy getOnNoTokenPolicy()
    {
        return this.defaultUnauthorizedConnector.getOnNoTokenPolicy();
    }

    @Override
    public void setOnNoTokenPolicy(OnNoTokenPolicy policy)
    {
        this.defaultUnauthorizedConnector.setOnNoTokenPolicy(policy);
    }

    @Override
    public String getDefaultAccessTokenId()
    {
        return this.defaultAccessTokenId;
    }

    public void setDefaultAccessTokenId(String defaultAccessTokenId)
    {
        this.defaultAccessTokenId = defaultAccessTokenId;
    }

    public void setRefreshTokenManager(RefreshTokenManager refreshTokenManager)
    {
        this.refreshTokenManager = refreshTokenManager;
    }

    protected void setAccessTokenPool(GenericKeyedObjectPool<String, OAuth2Adapter> accessTokenPool)
    {
        this.accessTokenPool = accessTokenPool;
    }
}
