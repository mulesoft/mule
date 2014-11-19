/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.security.oauth.OAuth1Adapter;
import org.mule.security.oauth.OAuth1Manager;
import org.mule.security.oauth.OAuthProperties;
import org.mule.security.oauth.callback.HttpCallbackAdapter;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseOAuth1AuthorizeMessageProcessor extends AbstractAuthorizeMessageProcessor
{

    private String requestTokenUrl = null;

    protected abstract Class<? extends OAuth1Adapter> getAdapterClass();

    @Override
    public void start() throws MuleException
    {
        super.start();
        OAuth1Adapter moduleObject = this.getAdapter();
        OAuth1Manager manager = moduleObject.getOauth1Manager();
        OAuth1FetchAccessTokenMessageProcessor fetchAccessTokenMessageProcessor = new OAuth1FetchAccessTokenMessageProcessor(
            moduleObject);

        if (this.getAccessTokenUrl() != null)
        {
            fetchAccessTokenMessageProcessor.setAccessTokenUrl(this.getAccessTokenUrl());
        }
        else
        {
            fetchAccessTokenMessageProcessor.setAccessTokenUrl(moduleObject.getAccessTokenUrl());
        }

        if (requestTokenUrl != null)
        {
            fetchAccessTokenMessageProcessor.setRequestTokenUrl(requestTokenUrl);
        }
        else
        {
            fetchAccessTokenMessageProcessor.setRequestTokenUrl(moduleObject.getRequestTokenUrl());
        }

        if (this.getAuthorizationUrl() != null)
        {
            fetchAccessTokenMessageProcessor.setAuthorizationUrl(this.getAuthorizationUrl());
        }
        else
        {
            fetchAccessTokenMessageProcessor.setAuthorizationUrl(moduleObject.getAuthorizationUrl());
        }

        this.startCallback(manager, fetchAccessTokenMessageProcessor);
    }

    @Override
    protected void startCallback(HttpCallbackAdapter adapter, FetchAccessTokenMessageProcessor fetchAccessTokenMessageProcessor) throws MuleException
    {
        callbackFactory.forceOldHttpTransport(true);
        super.startCallback(adapter, fetchAccessTokenMessageProcessor);
    }

    /**
     * Starts the OAuth authorization process
     * 
     * @param event MuleEvent to be processed
     * @throws MuleException
     */
    protected final MuleEvent doProcess(MuleEvent event) throws Exception
    {
        OAuth1Adapter moduleObject = this.getAdapter();
        Map<String, String> extraParameters = new HashMap<String, String>();

        if (this.getState() != null)
        {
            extraParameters.put("state", this.toString(event, this.getState()));
        }

        moduleObject.setAccessTokenUrl(this.toString(event, this.getAccessTokenUrl()));

        String location = moduleObject.authorize(extraParameters, requestTokenUrl, this.getAccessTokenUrl(),
            this.getAuthorizationUrl(), this.getOauthCallback().getUrl());

        this.notifyAuthorizeStart(event);

        event.getMessage().setOutboundProperty(OAuthProperties.HTTP_STATUS, "302");
        event.getMessage().setOutboundProperty(OAuthProperties.CALLBACK_LOCATION, location);

        return event;
    }

    protected OAuth1Adapter getAdapter()
    {
        try
        {
            Object maybeAnAdapter = this.findOrCreate(this.getAdapterClass(), false, null);
            if (!(maybeAnAdapter instanceof OAuth1Adapter))
            {
                throw new IllegalStateException(String.format(
                    "Object of class %s does not implement OAuth1Adapter", this.getAdapterClass()
                        .getCanonicalName()));
            }

            return (OAuth1Adapter) maybeAnAdapter;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets requestTokenUrl
     * 
     * @param value Value to set
     */
    public void setRequestTokenUrl(String value)
    {
        this.requestTokenUrl = value;
    }

    /**
     * Retrieves requestTokenUrl
     */
    public String getRequestTokenUrl()
    {
        return this.requestTokenUrl;
    }

}
