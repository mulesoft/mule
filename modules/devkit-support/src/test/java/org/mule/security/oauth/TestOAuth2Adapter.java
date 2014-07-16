/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.common.security.oauth.AuthorizationParameter;
import org.mule.common.security.oauth.exception.NotAuthorizedException;
import org.mule.common.security.oauth.exception.UnableToAcquireAccessTokenException;
import org.mule.common.security.oauth.exception.UnableToAcquireRequestTokenException;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class TestOAuth2Adapter
    implements OAuth2Adapter, Initialisable, Startable, Stoppable, MuleContextAware, Disposable
{

    private static final long serialVersionUID = -5278170237424242128L;

    private OAuth2Manager<OAuth2Adapter> manager;
    private MuleContext muleContext;

    private String name;
    private boolean postAuth = false;
    private boolean start = false;
    private boolean stop = false;
    private boolean initialise = false;
    private boolean disposed = false;

    private String consumerKey;
    private String consumerSecret;
    private String accessToken;
    private String authorizationUrl;
    private String accessTokenUrl;
    private String refreshToken;
    private OnNoTokenPolicy onNoTokenPolicy;

    public TestOAuth2Adapter(OAuth2Manager<OAuth2Adapter> manager)
    {
        this.manager = manager;
    }

    @Override
    public String getOauthVerifier()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setOauthVerifier(String value)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setAccessTokenUrl(String url)
    {
        this.accessTokenUrl = url;
    }

    @Override
    public void setAccessToken(String accessToken)
    {
        this.accessToken = accessToken;
    }

    @Override
    public void setAuthorizationUrl(String authorizationUrl)
    {
        this.authorizationUrl = authorizationUrl;
    }

    @Override
    public void hasBeenAuthorized() throws NotAuthorizedException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public String getAccessTokenUrl()
    {
        return this.accessTokenUrl;
    }

    @Override
    public String getConsumerKey()
    {
        return this.consumerKey;
    }

    @Override
    public String getConsumerSecret()
    {
        return this.consumerSecret;
    }

    @Override
    public String getScope()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getAuthorizationUrl()
    {
        return this.authorizationUrl;
    }

    @Override
    public String getAccessTokenRegex()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getExpirationRegex()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getRefreshTokenRegex()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getVerifierRegex()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<AuthorizationParameter<?>> getAuthorizationParameters()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void postAuth()
    {
        postAuth = true;
    }

    public boolean wasPostAuthCalled()
    {
        return postAuth;
    }

    public boolean wasDisposed()
    {
        return this.disposed;
    }

    public boolean wasInitialised()
    {
        return this.initialise;
    }

    public boolean wasStarted()
    {
        return this.start;
    }

    public boolean wasStopped()
    {
        return this.stop;
    }

    @Override
    public String authorize(Map<String, String> extraParameters, String accessTokenUrl, String redirectUri)
        throws UnableToAcquireRequestTokenException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void fetchAccessToken(String accessTokenUrl) throws UnableToAcquireAccessTokenException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean hasTokenExpired()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void refreshAccessToken(String accessTokenId) throws UnableToAcquireAccessTokenException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public String getAccessToken()
    {
        return this.accessToken;
    }

    @Override
    public Pattern getAccessCodePattern()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getRefreshToken()
    {
        return this.refreshToken;
    }

    @Override
    public void setRefreshToken(String refreshToken)
    {
        this.refreshToken = refreshToken;
    }

    @Override
    public Pattern getRefreshTokenPattern()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Pattern getExpirationTimePattern()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setExpiration(Date value)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setConsumerKey(String consumerKey)
    {
        this.consumerKey = consumerKey;
    }

    @Override
    public void setConsumerSecret(String consumerSecret)
    {
        this.consumerSecret = consumerSecret;
    }

    public OAuth2Manager<OAuth2Adapter> getManager()
    {
        return manager;
    }

    @Override
    public void stop() throws MuleException
    {
        this.stop = true;
    }

    @Override
    public void start() throws MuleException
    {
        this.start = true;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        this.initialise = true;
    }

    @Override
    public void dispose()
    {
        this.disposed = true;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public OnNoTokenPolicy getOnNoTokenPolicy()
    {
        return onNoTokenPolicy;
    }
    
    @Override
    public void setOnNoTokenPolicy(OnNoTokenPolicy onNoTokenPolicy)
    {
        this.onNoTokenPolicy = onNoTokenPolicy;
    }
    
}
