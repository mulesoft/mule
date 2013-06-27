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
import org.mule.api.store.ObjectDoesNotExistException;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.MessageFactory;
import org.mule.security.oauth.OAuth2Adapter;
import org.mule.security.oauth.OAuth2Manager;
import org.mule.security.oauth.OAuthProperties;
import org.mule.util.StringUtils;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OAuth2FetchAccessTokenMessageProcessor extends FetchAccessTokenMessageProcessor
{

    private static final Logger logger = LoggerFactory.getLogger(OAuth2FetchAccessTokenMessageProcessor.class);
    private static final Pattern EVENT_ID_PATTERN = Pattern.compile("<<MULE_EVENT_ID=([\\w-]*)>>");
    private static final Pattern ORIGINAL_STATE_PATTERN = Pattern.compile("<<MULE_EVENT_ID=[\\w-]*>>(.*)");

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
        this.notifyCallbackReception(event);
        MuleEvent restoredEvent = this.restoreOriginalEvent(event);

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

            restoredEvent.getMessage().setInvocationProperty(OAuthProperties.VERIFIER,
                event.getMessage().getInvocationProperty(OAuthProperties.VERIFIER));

            restoredEvent.getMessage().setInvocationProperty(OAuthProperties.ACCESS_TOKEN_ID,
                transformedAccessTokenId);
        }
        catch (Exception e)
        {
            throw new MessagingException(MessageFactory.createStaticMessage("Unable to fetch access token"),
                event, e);
        }

        return restoredEvent;
    }

    private MuleEvent restoreOriginalEvent(MuleEvent event) throws MuleException
    {
        String state = event.getMessage().getInboundProperty("state");

        String eventId = null;

        try
        {
            eventId = StringUtils.match(EVENT_ID_PATTERN, state, 1);
        }
        catch (IllegalArgumentException e)
        {
            throw new MessagingException(
                MessageFactory.createStaticMessage(String.format(
                    "Could not fetch original event for callback with state %s. Could not extract original event id",
                    state)), event);
        }

        if (logger.isDebugEnabled())
        {
            logger.debug(String.format("received callback for event id %s. Fetching original event", eventId));
        }

        MuleEvent restoredEvent = null;
        try
        {
            restoredEvent = this.oauthManager.restoreAuthorizationEvent(eventId);
        }
        catch (ObjectDoesNotExistException e)
        {
            throw new MessagingException(MessageFactory.createStaticMessage(String.format(
                "Could not find authorization event %s in object store", eventId)), event, e);
        }
        catch (ObjectStoreException e)
        {
            throw new MessagingException(MessageFactory.createStaticMessage(String.format(
                "Error retrieving authorization event %s from object store", eventId)), event, e);
        }

        try
        {
            restoredEvent.getMessage().setProperty("state",
                StringUtils.match(ORIGINAL_STATE_PATTERN, state, 1), PropertyScope.INBOUND);
        }
        catch (IllegalArgumentException e)
        {
            // user did not use the state at all, just blank it
            restoredEvent.getMessage().setProperty("state", StringUtils.EMPTY, PropertyScope.INBOUND);
        }

        return restoredEvent;
    }

}
