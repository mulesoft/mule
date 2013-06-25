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
import org.mule.config.i18n.MessageFactory;
import org.mule.security.oauth.OAuth2Adapter;
import org.mule.security.oauth.OAuth2Manager;
import org.mule.security.oauth.OAuthProperties;

import org.apache.commons.lang.StringUtils;

public class OAuth2FetchAccessTokenMessageProcessor extends FetchAccessTokenMessageProcessor
{

    private OAuth2Manager<OAuth2Adapter> oauthManager;

    public OAuth2FetchAccessTokenMessageProcessor(OAuth2Manager<OAuth2Adapter> oauthManager,
                                                  String accessTokenId)
    {
        this.oauthManager = oauthManager;
        this.setAccessTokenId(accessTokenId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected MuleEvent doProcess(MuleEvent event) throws Exception
    {
        try
        {
            OAuth2Adapter oauthAdapter = this.oauthManager.createAdapter(((String) event.getMessage()
                .getInvocationProperty(OAuthProperties.VERIFIER)));

            if (oauthAdapter.getAccessTokenUrl() == null)
            {
                oauthAdapter.setAccessTokenUrl(this.getAccessTokenUrl());
            }
            oauthAdapter.fetchAccessToken(this.getRedirectUri());

            String transformedAccessTokenId = StringUtils.isBlank(this.getAccessTokenId())
                                                                                          ? this.oauthManager.getDefaultUnauthorizedConnector()
                                                                                              .getName()
                                                                                          : this.getAccessTokenId();

            transformedAccessTokenId = (String) this.evaluateAndTransform(event.getMuleContext(), event,
                String.class, null, transformedAccessTokenId);

            this.oauthManager.getAccessTokenPoolFactory().passivateObject(transformedAccessTokenId,
                oauthAdapter);

            event.getMessage().setInvocationProperty(OAuthProperties.ACCESS_TOKEN_ID,
                transformedAccessTokenId);
        }
        catch (Exception e)
        {
            throw new MessagingException(MessageFactory.createStaticMessage("Unable to fetch access token"),
                event, e);
        }
        return event;
    }

}
