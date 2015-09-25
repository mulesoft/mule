/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.processor;

import static org.mule.module.http.api.HttpConstants.RequestProperties.HTTP_QUERY_PARAMS;

import org.mule.RequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.store.ObjectDoesNotExistException;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.MessageFactory;
import org.mule.module.http.api.HttpParameters;
import org.mule.security.oauth.OAuth2Adapter;
import org.mule.security.oauth.OAuth2Manager;
import org.mule.security.oauth.OAuthProperties;
import org.mule.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
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
        MuleEvent restoredEvent = this.restoreOriginalEvent(event);
        this.notifyCallbackReception(event);

        try
        {
            OAuth2Adapter oauthAdapter = this.oauthManager.createAdapter(restoredEvent, ((String) event.getMessage()
                .getInvocationProperty(OAuthProperties.VERIFIER)));

            if (oauthAdapter.getAccessTokenUrl() == null)
            {
                oauthAdapter.setAccessTokenUrl(this.getAccessTokenUrl());
            }
            oauthAdapter.fetchAccessToken(this.getRedirectUri());

            String transformedAccessTokenId = this.getAccessTokenId();

            if (StringUtils.isEmpty(transformedAccessTokenId))
            {
                transformedAccessTokenId = this.oauthManager.getDefaultUnauthorizedConnector().getName();
            }

            transformedAccessTokenId = (String) this.evaluateAndTransform(restoredEvent.getMuleContext(),
                restoredEvent, String.class, null, transformedAccessTokenId);

            this.oauthManager.getAccessTokenPoolFactory().passivateObject(transformedAccessTokenId,
                oauthAdapter);

            MuleMessage message = restoredEvent.getMessage();

            message.setInvocationProperty(OAuthProperties.VERIFIER,
                event.getMessage().getInvocationProperty(OAuthProperties.VERIFIER));

            message.setInvocationProperty(OAuthProperties.ACCESS_TOKEN_ID, transformedAccessTokenId);

            message.removeProperty(OAuthProperties.HTTP_STATUS, PropertyScope.OUTBOUND);
            message.removeProperty(OAuthProperties.CALLBACK_LOCATION, PropertyScope.OUTBOUND);
        }
        catch (Exception e)
        {
            throw new MessagingException(MessageFactory.createStaticMessage("Unable to fetch access token"),
                event, e, this);
        }

        return restoredEvent;
    }

    private MuleEvent restoreOriginalEvent(MuleEvent event) throws MuleException
    {
        String state = getState(event);
        if (StringUtils.isEmpty(state))
        {
            return event;
        }

        String eventId = StringUtils.match(EVENT_ID_PATTERN, state, 1);

        if (StringUtils.isBlank(eventId))
        {
            if (logger.isWarnEnabled())
            {
                logger.warn(String.format(
                    "Could not fetch original event id for callback with state %s. Will continue with new event without restoring previous one",
                    state));
            }
            return event;
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
            throw new MessagingException(MessageFactory.createStaticMessage(String.format("Could not find authorization event %s in object store", eventId)),
                event, e, this);
        }
        catch (ObjectStoreException e)
        {
            throw new MessagingException(MessageFactory.createStaticMessage(String.format("Error retrieving authorization event %s from object store", eventId)),
                event, e, this);
        }

        InboundPropertiesDelegate properties = getPropertiesDelegate(restoredEvent);
        String cleanedState = StringUtils.match(ORIGINAL_STATE_PATTERN, state, 1);

        if (cleanedState != null)
        {
            properties.set("state", cleanedState);
        }
        else
        {
            // user did not use the state at all, just blank it
            properties.set("state", StringUtils.EMPTY);
        }

        RequestContext.setEvent(restoredEvent);
        return restoredEvent;
    }

    private String getState(MuleEvent event) throws MuleException
    {
        String state = getPropertiesDelegate(event).get("state");

        try
        {
            return StringUtils.isEmpty(state) ? StringUtils.EMPTY : URLDecoder.decode(state, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new MessagingException(
                    MessageFactory.createStaticMessage("State query param had invalid encoding: " + state), event, this);
        }
    }

    private InboundPropertiesDelegate getPropertiesDelegate(MuleEvent event)
    {
        Object parameters = event.getMessage().getInboundProperty("http.query.params");
        return parameters instanceof HttpParameters ? new HttpConnectorDelegate(event) : new OldHttpTransport(event);
    }

    private interface InboundPropertiesDelegate
    {

        String get(String key);

        void set(String key, String value);
    }

    private class HttpConnectorDelegate implements InboundPropertiesDelegate
    {
        private final HashMap<String, String> parameters;

        private HttpConnectorDelegate(MuleEvent event)
        {
            //Query parameters are immutable so we copy them and change the query parameters map.
            this.parameters = new HashMap(((HttpParameters) event.getMessage().getInboundProperty(HTTP_QUERY_PARAMS)));
            event.getMessage().setProperty(HTTP_QUERY_PARAMS, this.parameters, PropertyScope.INBOUND);
        }

        @Override
        public String get(String key)
        {
            return parameters.get(key);
        }

        @Override
        public void set(String key, String value)
        {
            parameters.remove(key);
            parameters.put(key, value);
        }
    }

    private class OldHttpTransport implements InboundPropertiesDelegate
    {
        private final MuleEvent event;

        private OldHttpTransport(MuleEvent event)
        {
            this.event = event;
        }

        @Override
        public String get(String key)
        {
            return event.getMessage().getInboundProperty(key);
        }

        @Override
        public void set(String key, String value)
        {
            event.getMessage().setProperty(key, value, PropertyScope.INBOUND);
        }
    }
}
