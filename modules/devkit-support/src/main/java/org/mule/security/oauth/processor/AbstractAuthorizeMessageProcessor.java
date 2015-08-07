/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.callback.HttpCallback;
import org.mule.api.callback.HttpCallbackFactory;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transformer.TransformerMessagingException;
import org.mule.devkit.processor.DevkitBasedMessageProcessor;
import org.mule.module.http.internal.config.HttpConfiguration;
import org.mule.security.oauth.callback.DefaultHttpCallbackFactory;
import org.mule.security.oauth.callback.HttpCallbackAdapter;
import org.mule.security.oauth.notification.OAuthAuthorizeNotification;

public abstract class AbstractAuthorizeMessageProcessor extends DevkitBasedMessageProcessor
    implements FlowConstructAware, MuleContextAware, Initialisable, Startable, Stoppable,
    InterceptingMessageProcessor
{

    private MessageProcessor listener;
    private String authorizationUrl = null;
    private String accessTokenUrl = null;
    private HttpCallback oauthCallback;
    private String state;
    protected HttpCallbackFactory callbackFactory;

    public AbstractAuthorizeMessageProcessor()
    {
        super("authorize");
    }

    protected abstract String getAuthCodeRegex();

    @Override
    public void start() throws MuleException
    {
        if (callbackFactory == null)
        {
            callbackFactory = new DefaultHttpCallbackFactory();
            callbackFactory.forceOldHttpTransport(HttpConfiguration.useTransportForUris(muleContext));
        }
    }
    
    protected void startCallback(HttpCallbackAdapter adapter,
                                 FetchAccessTokenMessageProcessor fetchAccessTokenMessageProcessor)
        throws MuleException
    {
        if (this.oauthCallback == null)
        {
            this.oauthCallback = this.callbackFactory.createCallback(adapter, this.getAuthCodeRegex(),
                fetchAccessTokenMessageProcessor, this.listener, muleContext, flowConstruct);

            this.oauthCallback.start();
        }
        
        fetchAccessTokenMessageProcessor.setRedirectUri(this.oauthCallback.getUrl());
    }

    protected void notifyAuthorizeStart(MuleEvent event)
    {
        muleContext.fireNotification(new OAuthAuthorizeNotification(event,
            OAuthAuthorizeNotification.OAUTH_AUTHORIZATION_BEGIN));
    }

    @Override
    public final void stop() throws MuleException
    {
        if (this.oauthCallback != null)
        {
            this.oauthCallback.stop();
        }
    }

    protected String toString(MuleEvent event, Object value)
    {
        try
        {
            return (String) evaluateAndTransform(getMuleContext(), event, String.class, null, value);
        }
        catch (TransformerException e)
        {
            throw new RuntimeException(e);
        }
        catch (TransformerMessagingException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets listener
     * 
     * @param value Value to set
     */
    @Override
    public void setListener(MessageProcessor value)
    {
        this.listener = value;
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
     * Sets state
     * 
     * @param value Value to set
     */
    public void setState(String value)
    {
        this.state = value;
    }

    public HttpCallback getOauthCallback()
    {
        return oauthCallback;
    }

    public void setOauthCallback(HttpCallback oauthCallback)
    {
        this.oauthCallback = oauthCallback;
    }

    public MessageProcessor getListener()
    {
        return listener;
    }

    public String getState()
    {
        return state;
    }
    
    public void setCallbackFactory(HttpCallbackFactory callbackFactory)
    {
        this.callbackFactory = callbackFactory;
    }

}
