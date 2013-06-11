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

import org.mule.common.security.oauth.exception.NotAuthorizedException;
import org.mule.common.security.oauth.exception.UnableToAcquireAccessTokenException;
import org.mule.common.security.oauth.exception.UnableToAcquireRequestTokenException;
import org.mule.security.oauth.callback.HttpCallbackAdapter;

import java.util.Map;

public interface OAuth1Manager extends HttpCallbackAdapter
{

    public String buildAuthorizeUrl(OAuth1Adapter adapter,
                                    Map<String, String> extraParameters,
                                    String requestTokenUrl,
                                    String accessTokenUrl,
                                    String authorizationUrl,
                                    String redirectUri) throws UnableToAcquireRequestTokenException;

    public boolean restoreAccessToken(OAuth1Adapter adapter);

    public void fetchAccessToken(OAuth1Adapter adapter,
                                 String requestTokenUrl,
                                 String accessTokenUrl,
                                 String authorizationUrl,
                                 String redirectUri) throws UnableToAcquireAccessTokenException;

    public void hasBeenAuthorized(OAuth1Adapter adapter) throws NotAuthorizedException;
    
    public void reset(OAuth1Adapter adapter);

}
