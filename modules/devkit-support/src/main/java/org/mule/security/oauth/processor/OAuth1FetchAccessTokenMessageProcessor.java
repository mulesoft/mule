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
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.MessageFactory;
import org.mule.security.oauth.OAuth1Manager;

public class OAuth1FetchAccessTokenMessageProcessor implements MessageProcessor
{

    public String redirectUri;
    private String accessTokenUrl = null;
    private String authorizationUrl = null;
    private String requestTokenUrl = null;
    private OAuth1Manager oauthManager = null;

    public OAuth1FetchAccessTokenMessageProcessor(OAuth1Manager oauthManager)
    {
        this.oauthManager = oauthManager;
    }

    public final MuleEvent process(MuleEvent event) throws MuleException
    {
        try
        {
            this.oauthManager.setOauthVerifier(((String) event.getMessage().getInvocationProperty(
                "_oauthVerifier")));
            this.oauthManager.fetchAccessToken(requestTokenUrl, accessTokenUrl, authorizationUrl, redirectUri);
        }
        catch (Exception e)
        {
            throw new MessagingException(MessageFactory.createStaticMessage("Unable to fetch access token"),
                event, e);
        }
        return event;
    }

    /**
     * Sets redirectUri
     * 
     * @param value Value to set
     */
    public void setRedirectUri(String value)
    {
        this.redirectUri = value;
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
