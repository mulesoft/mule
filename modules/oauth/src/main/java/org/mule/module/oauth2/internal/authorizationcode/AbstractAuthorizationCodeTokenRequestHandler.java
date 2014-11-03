/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.authorizationcode;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.construct.Flow;
import org.mule.module.http.listener.HttpListener;
import org.mule.module.http.listener.HttpListenerBuilder;
import org.mule.module.oauth2.internal.AutoTokenRequestHandler;
import org.mule.module.oauth2.internal.authorizationcode.state.UserOAuthContext;

import java.net.MalformedURLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for token request handler.
 */
public abstract class AbstractAuthorizationCodeTokenRequestHandler extends AutoTokenRequestHandler
{

    protected Logger logger = LoggerFactory.getLogger(getClass());
    private AuthorizationCodeConfig oauthConfig;
    private HttpListener redirectUrlListener;


    /**
     * Updates the access token by calling the token url with refresh token grant type
     *
     * @param currentEvent the event at the moment of the failure.
     * @param resourceOwnerId the resource owner id to update
     */
    public void refreshToken(final MuleEvent currentEvent, String resourceOwnerId) throws MuleException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Executing refresh token for user " + resourceOwnerId);
        }
        final UserOAuthContext userOAuthContext = getOauthConfig().getUserOAuthContext().getContextForUser(resourceOwnerId);
        final boolean lockWasAcquired = userOAuthContext.getRefreshUserOAuthContextLock().tryLock();
        try
        {
            if (lockWasAcquired)
            {
                doRefreshToken(currentEvent, userOAuthContext);
            }
        }
        finally
        {
            if (lockWasAcquired)
            {
                userOAuthContext.getRefreshUserOAuthContextLock().unlock();
            }
        }
        if (!lockWasAcquired)
        {
            //if we couldn't acquire the lock then we wait until the other thread updates the token.
            waitUntilLockGetsReleased(userOAuthContext);
        }
    }

    /**
     * ThreadSafe refresh token operation to be implemented by subclasses
     *
     * @param currentEvent the event at the moment of the failure.
     * @param userOAuthContext user oauth context object.
     */
    protected abstract void doRefreshToken(final MuleEvent currentEvent, final UserOAuthContext userOAuthContext) throws MuleException;

    private void waitUntilLockGetsReleased(UserOAuthContext userOAuthContext)
    {
        userOAuthContext.getRefreshUserOAuthContextLock().lock();
        userOAuthContext.getRefreshUserOAuthContextLock().unlock();
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
            this.redirectUrlListener = new HttpListenerBuilder(getMuleContext())
                    .setUrl(getOauthConfig().getRedirectionUrl())
                    .setFlowConstruct(new Flow("some flow name", getMuleContext()))
                    .setMuleContext(getMuleContext())
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

}
