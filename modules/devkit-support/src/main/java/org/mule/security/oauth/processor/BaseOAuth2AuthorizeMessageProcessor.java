/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.security.oauth.processor;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.transformer.TransformerException;
import org.mule.common.security.oauth.AuthorizationParameter;
import org.mule.config.i18n.CoreMessages;
import org.mule.security.oauth.OAuth2Adapter;
import org.mule.security.oauth.OAuth2Manager;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class BaseOAuth2AuthorizeMessageProcessor<T extends OAuth2Manager<OAuth2Adapter>> extends
    AbstractAuthorizeMessageProcessor
{

    protected abstract Class<T> getOAuthManagerClass();

    @Override
    public final void start() throws MuleException
    {
        OAuth2Manager<OAuth2Adapter> module = this.getOAuthManager();
        FetchAccessTokenMessageProcessor fetchAccessTokenMessageProcessor = new OAuth2FetchAccessTokenMessageProcessor(
            (OAuth2Manager<OAuth2Adapter>) module, this.getAccessTokenId());

        this.startCallback(module, fetchAccessTokenMessageProcessor);

        if (this.getAccessTokenUrl() != null)
        {
            fetchAccessTokenMessageProcessor.setAccessTokenUrl(this.getAccessTokenUrl());
        }
        else
        {
            fetchAccessTokenMessageProcessor.setAccessTokenUrl(module.getDefaultUnauthorizedConnector()
                .getAccessTokenUrl());
        }
    }

    @SuppressWarnings("unchecked")
    protected OAuth2Manager<OAuth2Adapter> getOAuthManager()
    {
        try
        {
            Object maybeAManager = this.findOrCreate(this.getOAuthManagerClass(), false, null);
            if (!(maybeAManager instanceof OAuth2Manager))
            {
                throw new IllegalStateException(String.format(
                    "Object of class %s does not implement OAuth2Manager", this.getOAuthManagerClass()
                        .getCanonicalName()));
            }

            return (OAuth2Manager<OAuth2Adapter>) maybeAManager;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> getExtraParameters(MuleEvent event, OAuth2Manager<OAuth2Adapter> moduleObject)
        throws MessagingException, TransformerException
    {
        Set<AuthorizationParameter<?>> params = moduleObject.getDefaultUnauthorizedConnector()
            .getAuthorizationParameters();

        Map<String, String> extraParameters = new HashMap<String, String>();
        if (this.getState() != null)
        {
            extraParameters.put("state", this.toString(event, this.getState()));
        }

        if (params != null)
        {
            for (AuthorizationParameter<?> parameter : params)
            {
                Field field = null;
                try
                {
                    field = this.getClass().getDeclaredField(parameter.getName());
                }
                catch (NoSuchFieldException e)
                {
                    throw new MessagingException(CoreMessages.createStaticMessage(String.format(
                        "Code generation error. Field %s should be present in class", parameter.getName())),
                        event, e);
                }

                field.setAccessible(true);

                try
                {
                    Object value = field.get(this);
                    Object transformed = this.evaluateAndTransform(getMuleContext(), event,
                        parameter.getType(), null, value);
                    extraParameters.put(parameter.getName(), this.toString(event, transformed).toLowerCase());
                }
                catch (IllegalAccessException e)
                {
                    throw new RuntimeException(e);
                }

            }
        }

        return extraParameters;

    }

    /**
     * Starts the OAuth authorization process
     * 
     * @param event MuleEvent to be processed
     * @throws MuleException
     */
    @Override
    public final MuleEvent process(MuleEvent event) throws MuleException
    {
        try
        {
            OAuth2Manager<OAuth2Adapter> moduleObject = this.getOAuthManager();

            String transformedAuthorizationUrl = this.toString(event, this.getAuthorizationUrl());
            String transformedAccessTokenUrl = this.toString(event, this.getAccessTokenUrl());

            moduleObject.getDefaultUnauthorizedConnector().setAccessTokenUrl(transformedAccessTokenUrl);
            String location = moduleObject.buildAuthorizeUrl(this.getExtraParameters(event, moduleObject),
                transformedAuthorizationUrl, this.getOauthCallback().getUrl());

            event.getMessage().setOutboundProperty("http.status", "302");
            event.getMessage().setOutboundProperty("Location", location);

            return event;
        }
        catch (Exception e)
        {
            throw new MessagingException(CoreMessages.failedToInvoke("authorize"), event, e);
        }
    }

}
