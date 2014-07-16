/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.processor;

import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.devkit.processor.DevkitBasedMessageProcessor;
import org.mule.security.oauth.notification.OAuthAuthorizeNotification;

public abstract class FetchAccessTokenMessageProcessor extends DevkitBasedMessageProcessor
    implements MessageProcessor
{

    private String redirectUri;
    private String accessTokenUrl = null;

    public FetchAccessTokenMessageProcessor()
    {
        super("fetch-access-token");
    }

    protected void notifyCallbackReception(MuleEvent event)
    {
        muleContext.fireNotification(new OAuthAuthorizeNotification(event,
            OAuthAuthorizeNotification.OAUTH_AUTHORIZATION_END));
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

    public String getRedirectUri()
    {
        return redirectUri;
    }

}
