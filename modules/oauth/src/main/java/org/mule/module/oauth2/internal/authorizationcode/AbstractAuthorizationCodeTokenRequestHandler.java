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
import org.mule.module.http.api.listener.HttpListener;
import org.mule.module.http.api.listener.HttpListenerBuilder;
import org.mule.module.oauth2.internal.AbstractTokenRequestHandler;
import org.mule.module.oauth2.internal.DynamicFlowFactory;
import org.mule.module.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for token request handler.
 */
public abstract class AbstractAuthorizationCodeTokenRequestHandler extends AbstractTokenRequestHandler
{

    protected Logger logger = LoggerFactory.getLogger(getClass());
    private AuthorizationCodeGrantType oauthConfig;
    private HttpListener redirectUrlListener;


    /**
     * Updates the access token by calling the token url with refresh token grant type
     *
     * @param currentEvent    the event at the moment of the failure.
     * @param resourceOwnerId the resource owner id to update
     */
    public void refreshToken(final MuleEvent currentEvent, String resourceOwnerId) throws MuleException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Executing refresh token for user " + resourceOwnerId);
        }
        final ResourceOwnerOAuthContext resourceOwnerOAuthContext = getOauthConfig().getUserOAuthContext().getContextForResourceOwner(resourceOwnerId);
        final boolean lockWasAcquired = resourceOwnerOAuthContext.getRefreshUserOAuthContextLock().tryLock();
        try
        {
            if (lockWasAcquired)
            {
                doRefreshToken(currentEvent, resourceOwnerOAuthContext);
                getOauthConfig().getUserOAuthContext().updateResourceOwnerOAuthContext(resourceOwnerOAuthContext);
            }
        }
        finally
        {
            if (lockWasAcquired)
            {
                resourceOwnerOAuthContext.getRefreshUserOAuthContextLock().unlock();
            }
        }
        if (!lockWasAcquired)
        {
            //if we couldn't acquire the lock then we wait until the other thread updates the token.
            waitUntilLockGetsReleased(resourceOwnerOAuthContext);
        }
    }

    /**
     * ThreadSafe refresh token operation to be implemented by subclasses
     *
     * @param currentEvent              the event at the moment of the failure.
     * @param resourceOwnerOAuthContext user oauth context object.
     */
    protected abstract void doRefreshToken(final MuleEvent currentEvent, final ResourceOwnerOAuthContext resourceOwnerOAuthContext) throws MuleException;

    private void waitUntilLockGetsReleased(ResourceOwnerOAuthContext resourceOwnerOAuthContext)
    {
        resourceOwnerOAuthContext.getRefreshUserOAuthContextLock().lock();
        resourceOwnerOAuthContext.getRefreshUserOAuthContextLock().unlock();
    }

    /**
     * @param oauthConfig oauth config for this token request handler.
     */
    public void setOauthConfig(AuthorizationCodeGrantType oauthConfig)
    {
        this.setTlsContextFactory(oauthConfig.getTlsContext());
        this.oauthConfig = oauthConfig;
    }

    public AuthorizationCodeGrantType getOauthConfig()
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
            final String flowName = "OAuthRedirectUrlFlow" + getOauthConfig().getRedirectionUrl();
            final Flow redirectUrlFlow = DynamicFlowFactory.createDynamicFlow(getMuleContext(), flowName, Arrays.asList(createRedirectUrlProcessor()));
            final HttpListenerBuilder httpListenerBuilder = new HttpListenerBuilder(getMuleContext())
                    .setUrl(new URL(getOauthConfig().getRedirectionUrl()))
                    .setFlow(redirectUrlFlow);
            if (getOauthConfig().getTlsContext() != null)
            {
                httpListenerBuilder.setTlsContextFactory(getOauthConfig().getTlsContext());
            }
            this.redirectUrlListener = httpListenerBuilder.build();
            this.redirectUrlListener.initialise();
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
