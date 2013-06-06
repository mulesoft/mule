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

import org.mule.api.MetadataAware;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.ProcessAdapter;
import org.mule.api.ProcessTemplate;
import org.mule.api.capability.Capabilities;
import org.mule.api.capability.ModuleCapability;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
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
import org.mule.security.oauth.process.ManagedAccessTokenProcessTemplate;
import org.mule.util.IOUtils;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;

public abstract class BaseOAuthManager<C extends OAuthAdapter> extends DefaultHttpCallbackAdapter
    implements MuleContextAware, Initialisable, Capabilities, MetadataAware, OAuthManager<OAuthAdapter>,
    ProcessAdapter<OAuthAdapter>
{

    private static Log logger = LogFactory.getLog(BaseOAuthManager.class);

    private OAuthAdapter defaultUnauthorizedConnector;
    private String consumerKey;
    private String consumerSecret;
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
    private String authorizationUrl = null;
    private String accessTokenUrl = null;

    /**
     * Access Token Pool Factory
     */
    private KeyedPoolableObjectFactory accessTokenPoolFactory;

    /**
     * Access Token Pool
     */
    private GenericKeyedObjectPool accessTokenPool;

    /**
     * Creates a concrete instance of the OAuthAdapter that corresponds with this
     * OAuthManager
     * 
     * @return an instance of {@link org.mule.security.oauth.OAuthAdapter}
     */
    protected abstract OAuthAdapter instantiateAdapter();

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
    protected abstract KeyedPoolableObjectFactory createPoolFactory(OAuthManager<OAuthAdapter> oauthManager,
                                                                    ObjectStore<OAuthState> objectStore);

    /**
     * Populates the adapter with custom properties not accessible from the base
     * interface.
     * 
     * @param adapter an instance of {@link org.mule.security.oauth.OAuthAdapter}
     */
    protected abstract void setCustomProperties(OAuthAdapter adapter);

    /**
     * Retrieves defaultUnauthorizedConnector
     */
    public OAuthAdapter getDefaultUnauthorizedConnector()
    {
        return this.defaultUnauthorizedConnector;
    }

    /**
     * Sets consumerKey
     * 
     * @param value Value to set
     */
    public void setConsumerKey(String value)
    {
        this.consumerKey = value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getConsumerKey()
    {
        return this.consumerKey;
    }

    /**
     * Sets consumerSecret
     * 
     * @param value Value to set
     */
    public void setConsumerSecret(String value)
    {
        this.consumerSecret = value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getConsumerSecret()
    {
        return this.consumerSecret;
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
     * Sets authorizationUrl
     * 
     * @param value Value to set
     */
    public void setAuthorizationUrl(String value)
    {
        this.authorizationUrl = value;
    }

    /**
     * Retrieves authorizationUrl
     */
    public String getAuthorizationUrl()
    {
        return this.authorizationUrl;
    }

    /**
     * Sets accessTokenUrl
     * 
     * @param value Value to set
     */
    public void setAccessTokenUrl(String value)
    {
        this.accessTokenUrl = value;
    }

    /**
     * Retrieves accessTokenUrl
     */
    public String getAccessTokenUrl()
    {
        return this.accessTokenUrl;
    }

    /**
     * {@inheritDoc}
     */
    public KeyedPoolableObjectFactory getAccessTokenPoolFactory()
    {
        return this.accessTokenPoolFactory;
    }

    public final void initialise() throws InitialisationException
    {
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
    }

    public final void start() throws MuleException
    {
        if (defaultUnauthorizedConnector instanceof Startable)
        {
            ((Startable) defaultUnauthorizedConnector).start();
        }
    }

    public final void stop() throws MuleException
    {
        if (defaultUnauthorizedConnector instanceof Stoppable)
        {
            ((Stoppable) defaultUnauthorizedConnector).stop();
        }
    }

    public final void dispose()
    {
        if (defaultUnauthorizedConnector instanceof Disposable)
        {
            ((Disposable) defaultUnauthorizedConnector).dispose();
        }
    }

    public final OAuthAdapter createAccessToken(String verifier) throws Exception
    {
        OAuthAdapter connector = this.instantiateAdapter();
        connector.setOauthVerifier(verifier);
        connector.setAuthorizationUrl(getAuthorizationUrl());
        connector.setAccessTokenUrl(getAccessTokenUrl());
        connector.setConsumerKey(getConsumerKey());
        connector.setConsumerSecret(getConsumerSecret());

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

    public final OAuthAdapter acquireAccessToken(String userId) throws Exception
    {
        if (logger.isDebugEnabled())
        {
            StringBuilder messageStringBuilder = new StringBuilder();
            messageStringBuilder.append("Pool Statistics before acquiring [key ");
            messageStringBuilder.append(userId);
            messageStringBuilder.append("] [active=");
            messageStringBuilder.append(accessTokenPool.getNumActive(userId));
            messageStringBuilder.append("] [idle=");
            messageStringBuilder.append(accessTokenPool.getNumIdle(userId));
            messageStringBuilder.append("]");
            logger.debug(messageStringBuilder.toString());
        }
        OAuthAdapter object = ((OAuthAdapter) accessTokenPool.borrowObject(userId));
        if (logger.isDebugEnabled())
        {
            StringBuilder messageStringBuilder = new StringBuilder();
            messageStringBuilder.append("Pool Statistics after acquiring [key ");
            messageStringBuilder.append(userId);
            messageStringBuilder.append("] [active=");
            messageStringBuilder.append(accessTokenPool.getNumActive(userId));
            messageStringBuilder.append("] [idle=");
            messageStringBuilder.append(accessTokenPool.getNumIdle(userId));
            messageStringBuilder.append("]");
            logger.debug(messageStringBuilder.toString());
        }
        return object;
    }

    public final void releaseAccessToken(String userId, OAuthAdapter connector) throws Exception
    {
        if (logger.isDebugEnabled())
        {
            StringBuilder messageStringBuilder = new StringBuilder();
            messageStringBuilder.append("Pool Statistics before releasing [key ");
            messageStringBuilder.append(userId);
            messageStringBuilder.append("] [active=");
            messageStringBuilder.append(accessTokenPool.getNumActive(userId));
            messageStringBuilder.append("] [idle=");
            messageStringBuilder.append(accessTokenPool.getNumIdle(userId));
            messageStringBuilder.append("]");
            logger.debug(messageStringBuilder.toString());
        }
        accessTokenPool.returnObject(userId, connector);
        if (logger.isDebugEnabled())
        {
            StringBuilder messageStringBuilder = new StringBuilder();
            messageStringBuilder.append("Pool Statistics after releasing [key ");
            messageStringBuilder.append(userId);
            messageStringBuilder.append("] [active=");
            messageStringBuilder.append(accessTokenPool.getNumActive(userId));
            messageStringBuilder.append("] [idle=");
            messageStringBuilder.append(accessTokenPool.getNumIdle(userId));
            messageStringBuilder.append("]");
            logger.debug(messageStringBuilder.toString());
        }
    }

    public final void destroyAccessToken(String userId, OAuthAdapter connector) throws Exception
    {
        if (logger.isDebugEnabled())
        {
            StringBuilder messageStringBuilder = new StringBuilder();
            messageStringBuilder.append("Pool Statistics before destroying [key ");
            messageStringBuilder.append(userId);
            messageStringBuilder.append("] [active=");
            messageStringBuilder.append(accessTokenPool.getNumActive(userId));
            messageStringBuilder.append("] [idle=");
            messageStringBuilder.append(accessTokenPool.getNumIdle(userId));
            messageStringBuilder.append("]");
            logger.debug(messageStringBuilder.toString());
        }
        accessTokenPool.invalidateObject(userId, connector);
        if (logger.isDebugEnabled())
        {
            StringBuilder messageStringBuilder = new StringBuilder();
            messageStringBuilder.append("Pool Statistics after destroying [key ");
            messageStringBuilder.append(userId);
            messageStringBuilder.append("] [active=");
            messageStringBuilder.append(accessTokenPool.getNumActive(userId));
            messageStringBuilder.append("] [idle=");
            messageStringBuilder.append(accessTokenPool.getNumIdle(userId));
            messageStringBuilder.append("]");
            logger.debug(messageStringBuilder.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String buildAuthorizeUrl(Map<String, String> extraParameters,
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
            urlBuilder.append(this.authorizationUrl);
        }
        urlBuilder.append("?");
        urlBuilder.append("response_type=code&");
        urlBuilder.append("client_id=");
        urlBuilder.append(getConsumerKey());
        urlBuilder.append("&redirect_uri=");
        urlBuilder.append(redirectUri);
        for (String parameter : extraParameters.keySet())
        {
            urlBuilder.append("&");
            urlBuilder.append(parameter);
            urlBuilder.append("=");
            urlBuilder.append(extraParameters.get(parameter));
        }

        if (logger.isDebugEnabled())
        {
            logger.debug(("Authorization URL has been generated as follows: " + urlBuilder));
        }

        return urlBuilder.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean restoreAccessToken(OAuthAdapter adapter, RestoreAccessTokenCallback callback)
    {
        if (callback != null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Attempting to restore access token...");
            }

            try
            {
                callback.restoreAccessToken();
                String accessToken = callback.getAccessToken();
                adapter.setAccessToken(accessToken);

                if (logger.isDebugEnabled())
                {
                    logger.debug(String.format("Access token and secret has been restored successfully [accessToken = %s]", accessToken));
                }
                return true;
            }
            catch (Exception e)
            {
                logger.error("Cannot restore access token, an unexpected error occurred", e);
            }
        }
        return false;
    }

    private void fetchAndExtract(OAuthAdapter adapter,
                                 RestoreAccessTokenCallback restoreCallback,
                                 SaveAccessTokenCallback saveCallback,
                                 String accessTokenUrl,
                                 String requestBodyParam,
                                 Pattern accessCodePattern) throws UnableToAcquireAccessTokenException
    {
        this.restoreAccessToken(adapter, restoreCallback);
        if (adapter.getAccessToken() == null)
        {
            try
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Retrieving access token...");
                }

                HttpURLConnection conn = ((HttpURLConnection) new URL(accessTokenUrl).openConnection());
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                if (logger.isDebugEnabled())
                {
                    logger.debug(String.format("Sending request to [%s] using the following as content [%s]", accessTokenUrl, requestBodyParam));
                }

                OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
                out.write(requestBodyParam);
                out.close();
                String response = IOUtils.toString(conn.getInputStream());

                if (logger.isDebugEnabled())
                {
                    logger.debug(String.format("Received response [%s]", response));
                }

                Matcher matcher = accessCodePattern.matcher(response);
                if (matcher.find() && (matcher.groupCount() >= 1))
                {
                    adapter.setAccessToken(URLDecoder.decode(matcher.group(1), "UTF-8"));

                    if (logger.isDebugEnabled())
                    {
                        logger.debug(String.format("Access token retrieved successfully [accessToken = %s]", adapter.getAccessToken()));
                    }
                    
                    if (saveCallback != null)
                    {
                        try
                        {
                            if (logger.isDebugEnabled())
                            {
                                logger.debug(String.format("Attempting to save access token...[accessToken = %s]", adapter.getAccessToken()));
                            }
                            saveCallback.saveAccessToken(adapter.getAccessToken(), null);
                        }
                        catch (Exception e)
                        {
                            logger.error("Cannot save access token, an unexpected error occurred", e);
                        }
                    }
                    
                    if (logger.isDebugEnabled())
                    {
                        StringBuilder messageStringBuilder = new StringBuilder();
                        messageStringBuilder.append("Attempting to extract expiration time using ");
                        messageStringBuilder.append("[expirationPattern = ");
                        messageStringBuilder.append("\"expires_in\"")
                        :([^&]+?),");
                        messageStringBuilder.append("] ");
                        LOGGER.debug(messageStringBuilder.toString());
                    }
                    Matcher expirationMatcher = EXPIRATION_TIME_PATTERN.matcher(response);
                    if (expirationMatcher.find() && (expirationMatcher.groupCount() >= 1))
                    {
                        Long expirationSecsAhead = Long.parseLong(expirationMatcher.group(1));
                        expiration = new Date((System.currentTimeMillis() + (expirationSecsAhead * 1000)));
                        if (LOGGER.isDebugEnabled())
                        {
                            StringBuilder messageStringBuilder = new StringBuilder();
                            messageStringBuilder.append("Token expiration extracted successfully ");
                            messageStringBuilder.append("[expiration = ");
                            messageStringBuilder.append(expiration);
                            messageStringBuilder.append("] ");
                            LOGGER.debug(messageStringBuilder.toString());
                        }
                    }
                    else
                    {
                        if (LOGGER.isDebugEnabled())
                        {
                            StringBuilder messageStringBuilder = new StringBuilder();
                            messageStringBuilder.append("Token expiration could not be extracted from ");
                            messageStringBuilder.append("[response = ");
                            messageStringBuilder.append(response);
                            messageStringBuilder.append("] ");
                            LOGGER.debug(messageStringBuilder.toString());
                        }
                    }
                    if (LOGGER.isDebugEnabled())
                    {
                        StringBuilder messageStringBuilder = new StringBuilder();
                        messageStringBuilder.append("Attempting to extract refresh token time using ");
                        messageStringBuilder.append("[refreshTokenPattern = ");
                        messageStringBuilder.append("\"refresh_token\":\"([^&]+?)\"");
                        messageStringBuilder.append("] ");
                        LOGGER.debug(messageStringBuilder.toString());
                    }
                    Matcher refreshTokenMatcher = REFRESH_TOKEN_PATTERN.matcher(response);
                    if (refreshTokenMatcher.find() && (refreshTokenMatcher.groupCount() >= 1))
                    {
                        refreshToken = refreshTokenMatcher.group(1);
                        if (LOGGER.isDebugEnabled())
                        {
                            StringBuilder messageStringBuilder = new StringBuilder();
                            messageStringBuilder.append("Refresh token extracted successfully ");
                            messageStringBuilder.append("[refresh token = ");
                            messageStringBuilder.append(refreshToken);
                            messageStringBuilder.append("] ");
                            LOGGER.debug(messageStringBuilder.toString());
                        }
                    }
                    else
                    {
                        if (LOGGER.isDebugEnabled())
                        {
                            StringBuilder messageStringBuilder = new StringBuilder();
                            messageStringBuilder.append("Refresh token could not be extracted from ");
                            messageStringBuilder.append("[response = ");
                            messageStringBuilder.append(response);
                            messageStringBuilder.append("] ");
                            LOGGER.debug(messageStringBuilder.toString());
                        }
                    }
                    fetchCallbackParameters(response);
                    postAuthorize();
                }
                else
                {
                    throw new Exception(String.format("OAuth access token could not be extracted from: %s",
                        response));
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
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
    public final <P> ProcessTemplate<P, OAuthAdapter> getProcessTemplate()
    {
        return new ManagedAccessTokenProcessTemplate<P>(this, getMuleContext());
    }

    public final String authorize(Map<String, String> extraParameters,
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
            urlBuilder.append(this.authorizationUrl);
        }
        urlBuilder.append("?");
        urlBuilder.append("response_type=code&");
        urlBuilder.append("client_id=");
        urlBuilder.append(getConsumerKey());
        urlBuilder.append("&redirect_uri=");
        urlBuilder.append(redirectUri);
        String scope = getScope();
        if (scope != null)
        {
            urlBuilder.append("&scope=");
            urlBuilder.append(scope);
        }
        for (String parameter : extraParameters.keySet())
        {
            urlBuilder.append("&");
            urlBuilder.append(parameter);
            urlBuilder.append("=");
            urlBuilder.append(extraParameters.get(parameter));
        }
        logger.debug(("Authorization URL has been generated as follows: " + urlBuilder));
        return urlBuilder.toString();
    }

}
