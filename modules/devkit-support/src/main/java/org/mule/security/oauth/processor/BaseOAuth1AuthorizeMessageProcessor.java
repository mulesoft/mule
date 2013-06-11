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

import org.mule.api.DefaultMuleException;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.CoreMessages;
import org.mule.security.oauth.DefaultHttpCallback;
import org.mule.security.oauth.HttpCallback;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class BaseOAuth1AuthorizeMessageProcessor extends
    AbstractDevkitBasedMessageProcessor
    implements FlowConstructAware, MuleContextAware, Initialisable, Startable, Stoppable,
    InterceptingMessageProcessor
{

    private MessageProcessor listener;
    private String authorizationUrl = null;
    private String accessTokenUrl = null;
    private String requestTokenUrl = null;
    private HttpCallback oauthCallback;
    private String state;

    protected abstract String getAuthCodeRegex();
    
    @Override
    public void start() throws MuleException
    {
        LinkedInConnectorOAuth1Adapter moduleObject = null;
        try
        {
//            moduleObject = findOrCreate(LinkedInConnectorOAuth1Adapter.class, false, null);
        }
        catch (IllegalAccessException e)
        {
            throw new DefaultMuleException(CoreMessages.failedToStart("authorize"), e);
        }
        catch (InstantiationException e)
        {
            throw new DefaultMuleException(CoreMessages.failedToStart("authorize"), e);
        }
        if (oauthCallback == null)
        {
            OAuth1FetchAccessTokenMessageProcessor fetchAccessTokenMessageProcessor = new OAuth1FetchAccessTokenMessageProcessor(
                moduleObject);
            oauthCallback = new DefaultHttpCallback(Arrays.asList(
                new ExtractAuthorizationCodeMessageProcessor(Pattern.compile(this.getAuthCodeRegex())),
                fetchAccessTokenMessageProcessor, listener), getMuleContext(), moduleObject.getDomain(),
                moduleObject.getLocalPort(), moduleObject.getRemotePort(), moduleObject.getPath(),
                moduleObject.getAsync(), getFlowConstruct().getExceptionListener(),
                moduleObject.getConnector());
            fetchAccessTokenMessageProcessor.setRedirectUri(oauthCallback.getUrl());
            if (accessTokenUrl != null)
            {
                fetchAccessTokenMessageProcessor.setAccessTokenUrl(accessTokenUrl);
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
            if (authorizationUrl != null)
            {
                fetchAccessTokenMessageProcessor.setAuthorizationUrl(authorizationUrl);
            }
            else
            {
                fetchAccessTokenMessageProcessor.setAuthorizationUrl(moduleObject.getAuthorizationUrl());
            }
            oauthCallback.start();
        }
    }

    public void stop() throws MuleException
    {
        if (oauthCallback != null)
        {
            oauthCallback.stop();
        }
    }

    /**
     * Starts the OAuth authorization process
     * 
     * @param event MuleEvent to be processed
     * @throws MuleException
     */
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        LinkedInConnectorOAuth1Adapter moduleObject = null;
        try
        {
            moduleObject = findOrCreate(LinkedInConnectorOAuth1Adapter.class, false, null);
            Map<String, String> extraParameters = new HashMap<String, String>();
            if (state != null)
            {
                try
                {
                    String transformerState = ((String) evaluateAndTransform(getMuleContext(), event,
                        AuthorizeMessageProcessor.class.getDeclaredField("state").getGenericType(), null,
                        state));
                    extraParameters.put("state", transformerState);
                }
                catch (NoSuchFieldException e)
                {
                    throw new MessagingException(CoreMessages.createStaticMessage("internal error"), event, e);
                }
            }
            String transformedAuthorizationUrl = ((String) evaluateAndTransform(getMuleContext(), event,
                AuthorizeMessageProcessor.class.getDeclaredField("authorizationUrl").getGenericType(), null,
                authorizationUrl));
            String transformedAccessTokenUrl = ((String) evaluateAndTransform(getMuleContext(), event,
                AuthorizeMessageProcessor.class.getDeclaredField("accessTokenUrl").getGenericType(), null,
                accessTokenUrl));
            moduleObject.setAccessTokenUrl(transformedAccessTokenUrl);
            String location = moduleObject.authorize(extraParameters, requestTokenUrl, accessTokenUrl,
                authorizationUrl, oauthCallback.getUrl());
            event.getMessage().setOutboundProperty("http.status", "302");
            event.getMessage().setOutboundProperty("Location", location);
            return event;
        }
        catch (Exception e)
        {
            throw new MessagingException(CoreMessages.failedToInvoke("authorize"), event, e);
        }
    }
    
    /**
     * Sets listener
     * 
     * @param value Value to set
     */
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

    /**
     * Sets state
     * 
     * @param value Value to set
     */
    public void setState(String value)
    {
        this.state = value;
    }

}
