/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.processor.MessageProcessor;
import org.mule.construct.Flow;
import org.mule.module.http.listener.HttpListener;
import org.mule.module.http.listener.HttpListenerBuilder;
import org.mule.module.oauth2.internal.state.UserOAuthState;

import java.net.MalformedURLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for token request handler.
 */
public abstract class AbstractTokenRequestHandler implements MuleContextAware
{

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private String refreshTokenWhen = "#[message.inboundProperties['http.status'] == 401 || message.inboundProperties['http.status'] == 403]";
    private AuthorizationCodeConfig oauthConfig;
    private HttpListener redirectUrlListener;
    private MuleContext muleContext;

    /**
     * @param refreshTokenWhen expression to use to determine if the response from a request to the API requires a new token
     */
    public void setRefreshTokenWhen(String refreshTokenWhen)
    {
        this.refreshTokenWhen = refreshTokenWhen;
    }

    public String getRefreshTokenWhen()
    {
        return refreshTokenWhen;
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    /**
     * Updates the access token by calling the token url with refresh token grant type
     *
     * @param currentEvent the event at the moment of the failure.
     * @param oauthStateId the oauth state id to update
     */
    public void refreshToken(final MuleEvent currentEvent, String oauthStateId) throws MuleException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Executing refresh token for user " + oauthStateId);
        }
        final UserOAuthState userOAuthState = getOauthConfig().getOAuthState().getStateForUser(oauthStateId);
        final boolean lockWasAcquired = userOAuthState.getRefreshUserOAuthStateLock().tryLock();
        try
        {
            if (lockWasAcquired)
            {
                doRefreshToken(currentEvent, userOAuthState);
            }
        }
        finally
        {
            if (lockWasAcquired)
            {
                userOAuthState.getRefreshUserOAuthStateLock().unlock();
            }
        }
        if (!lockWasAcquired)
        {
            //if we couldn't acquire the lock then we wait until the other thread updates the token.
            waitUntilLockGetsReleased(userOAuthState);
        }
    }

    /**
     * ThreadSafe refresh token operation to be implemented by subclasses
     *
     * @param currentEvent the event at the moment of the failure.
     * @param userOAuthState user oauth state object.
     */
    protected abstract void doRefreshToken(final MuleEvent currentEvent, final UserOAuthState userOAuthState) throws MuleException;

    private void waitUntilLockGetsReleased(UserOAuthState userOAuthState)
    {
        userOAuthState.getRefreshUserOAuthStateLock().lock();
        userOAuthState.getRefreshUserOAuthStateLock().unlock();
    }

    /**
     * @param oauthConfig oauth config for this token request handler.
     */
    public void setOauthConfig(AuthorizationCodeConfig oauthConfig)
    {
        this.oauthConfig = oauthConfig;
    }

    public AuthorizationCodeConfig getOauthConfig()
    {
        return oauthConfig;
    }

    /**
     * initialization method after configuration.
     */
    public void init() throws MuleException
    {
    }

    protected void createListenerForRedirectUrl() throws MuleException
    {
        try
        {
            this.redirectUrlListener = new HttpListenerBuilder(muleContext)
                    .setUrl(getOauthConfig().getRedirectionUrl())
                    .setFlowConstruct(new Flow("some flow name", muleContext))
                    .setMuleContext(muleContext)
                    .setListenerConfig(getOauthConfig().getListenerConfig())
                    .setListener(createRedirectUrlProcessor()).build();
            this.redirectUrlListener.start();
        }
        catch (MalformedURLException e)
        {
            logger.warn("Could not parse provided url %s. Validate that the url is correct", getOauthConfig().getRedirectionUrl());
            throw new DefaultMuleException(e);
        }
    }

    protected abstract MessageProcessor createRedirectUrlProcessor();

    public MuleContext getMuleContext()
    {
        return muleContext;
    }
}
