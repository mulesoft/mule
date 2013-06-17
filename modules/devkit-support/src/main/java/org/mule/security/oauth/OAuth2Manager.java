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
import org.mule.common.security.oauth.exception.UnableToAcquireAccessTokenException;
import org.mule.security.oauth.callback.HttpCallbackAdapter;

import java.util.Map;

import org.apache.commons.pool.KeyedPoolableObjectFactory;

/**
 * Wrapper around {@link org.mule.api.annotations.oauth.OAuth} annotated class that
 * will infuse it with access token management capabilities.
 * <p/>
 * It can receive a {@link org.mule.config.PoolingProfile} which is a configuration
 * object used to define the OAuth access tokens pooling parameters.
 * 
 * @param <C> Actual connector object that represents a connection
 */
public interface OAuth2Manager<C extends OAuth2Adapter> extends HttpCallbackAdapter
{

    /**
     * Create a new adapter using the specified verifier and insert it into the
     * pool. This adapter will be already initialized and started
     * 
     * @param verifier OAuth verifier
     * @return A newly created connector
     * @throws Exception If the access token cannot be retrieved
     */
    C createAdapter(String verifier) throws Exception;

    /**
     * Borrow an access token from the pool
     * 
     * @param userId User identification used to borrow the access token
     * @return An existing authorized connector
     * @throws Exception If the access token cannot be retrieved
     */
    C acquireAccessToken(String userId) throws Exception;

    /**
     * Return an access token to the pool
     * 
     * @param userId User identification used to borrow the access token
     * @param connector Authorized connector to be returned to the pool
     * @throws Exception If the access token cannot be returned
     */
    void releaseAccessToken(String userId, C connector) throws Exception;

    /**
     * Destroy an access token
     * 
     * @param userId User identification used to borrow the access token
     * @param connector Authorized connector to the destroyed
     * @throws Exception If the access token could not be destroyed.
     */
    void destroyAccessToken(String userId, C connector) throws Exception;

    /**
     * Retrieve default unauthorized connector
     */
    C getDefaultUnauthorizedConnector();

    /**
     * Retrieves accessTokenPoolFactory
     */
    public KeyedPoolableObjectFactory getAccessTokenPoolFactory();

    /**
     * Generates the full URL of an authorization endpoint including query params
     * 
     * @param extraParameters a map with non-standard query-param value pairs
     * @param authorizationUrl the url of the authorization endpoint per OAuth
     *            specification
     * @param redirectUri the uri of the redirection endpoint
     * @return the authorization URL as a String
     */
    public String buildAuthorizeUrl(Map<String, String> extraParameters,
                                    String authorizationUrl,
                                    String redirectUri);

    /**
     * Tries to use the adapter's restore an access token to retrieve the token. If
     * the token is successfuly retrieved then it is set into the given adapter
     * 
     * @param adapter the adapter on which the access token will be set upon success
     * @return <code>true</code> if a token could be retrieved and set into the
     *         adapter. <code>false</code> otherwise
     */
    public boolean restoreAccessToken(OAuth2Adapter adapter);

    /**
     * if refresh token is available, then it makes an http call to refresh the access token.
     * All newly obtained tokens are set into the adapter
     *  
     * @param adapter the connector's adapter
     * @throws UnableToAcquireAccessTokenException
     */
    public void refreshAccessToken(OAuth2Adapter adapter) throws UnableToAcquireAccessTokenException;

    /**
     * Makes an http call to the adapter's accessTokenUrl and extracts the access
     * token, which is then set into the adapter
     * 
     * @param adapter the connector's adapter
     * @param redirectUri the redirection URI
     * @throws UnableToAcquireAccessTokenException
     */
    public void fetchAccessToken(OAuth2Adapter adapter, String redirectUri)
        throws UnableToAcquireAccessTokenException;
    
    /**
     * Returns the mule context
     */
    public MuleContext getMuleContext();

}
