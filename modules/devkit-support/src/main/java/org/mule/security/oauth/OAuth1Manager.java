/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth;

import org.mule.api.transport.Connector;
import org.mule.common.security.oauth.exception.NotAuthorizedException;
import org.mule.common.security.oauth.exception.UnableToAcquireAccessTokenException;
import org.mule.common.security.oauth.exception.UnableToAcquireRequestTokenException;
import org.mule.security.oauth.callback.HttpCallbackAdapter;

import java.util.Map;

public interface OAuth1Manager extends HttpCallbackAdapter
{

    /**
     * Builds the authorization url to initiate the OAuth dance
     * 
     * @param adapter the adapter that is going to be authorized
     * @param extraParameters provider specific extra parameters
     * @param requestTokenUrl the url of the request token server
     * @param accessTokenUrl the url of the access token server
     * @param authorizationUrl the url of the authorization server
     * @param redirectUri the redirection uri
     * @return a String with the authorization url
     * @throws UnableToAcquireRequestTokenException
     */
    public String buildAuthorizeUrl(OAuth1Adapter adapter,
                                    Map<String, String> extraParameters,
                                    String requestTokenUrl,
                                    String accessTokenUrl,
                                    String authorizationUrl,
                                    String redirectUri) throws UnableToAcquireRequestTokenException;

    /**
     * Restores the access token that belongs to the given adapter and sets its value
     * into it. The restoration relies on the adapter's restore callback. If it isn't
     * set, then no restoration is performed
     * 
     * @param adapter the adapter which access token is to be restored
     * @return <code>true</code> if the access token was succesfully restored.
     *         <code>false</code> otherwise.
     */
    public boolean restoreAccessToken(OAuth1Adapter adapter);

    /**
     * Retrieves the access token for the given adapter with the given parameters. The obtained token is set into the adapter 
     * @param adapter the adapter which access token you want
     * @param requestTokenUrl the url of the request token server
     * @param accessTokenUrl the url of the access token server
     * @param authorizationUrl the url of the authorization server
     * @param redirectUri the redirection uri
     * @throws UnableToAcquireAccessTokenException
     */
    public void fetchAccessToken(OAuth1Adapter adapter,
                                 String requestTokenUrl,
                                 String accessTokenUrl,
                                 String authorizationUrl,
                                 String redirectUri) throws UnableToAcquireAccessTokenException;

    /**
     * Determines if the adapter has been authorized or not by checking its access token
     * @param adapter the adapter to be checked
     * @throws NotAuthorizedException if the adapter hasn't been authorized
     */
    public void hasBeenAuthorized(OAuth1Adapter adapter) throws NotAuthorizedException;

    /**
     * Sets the adapter to a blank, unatuhorized state
     * @param adapter the adapter to be reset
     */
    public void reset(OAuth1Adapter adapter);

    public Connector getConnector();

}
