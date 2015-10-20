/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.processor;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.config.i18n.MessageFactory;
import org.mule.security.oauth.OAuth1Adapter;

public class OAuth1FetchAccessTokenMessageProcessor extends FetchAccessTokenMessageProcessor
{

    private String authorizationUrl = null;
    private String requestTokenUrl = null;
    private OAuth1Adapter adapter = null;

    public OAuth1FetchAccessTokenMessageProcessor(OAuth1Adapter adapter)
    {
        this.adapter = adapter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final MuleEvent doProcess(MuleEvent event) throws Exception
    {
        this.notifyCallbackReception(event);
        try
        {
            this.adapter.setOauthVerifier(((String) event.getMessage()
                .getInvocationProperty("_oauthVerifier")));
            this.adapter.fetchAccessToken(requestTokenUrl, this.getAccessTokenUrl(), authorizationUrl,
                this.getRedirectUri());
        }
        catch (Exception e)
        {
            throw new MessagingException(MessageFactory.createStaticMessage("Unable to fetch access token"),
                event, e, this);
        }
        return event;
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
