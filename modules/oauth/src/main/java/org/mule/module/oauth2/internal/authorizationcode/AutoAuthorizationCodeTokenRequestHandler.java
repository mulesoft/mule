/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.authorizationcode;

import org.mule.DefaultMuleEvent;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.http.listener.MessageProperties;
import org.mule.module.oauth2.internal.NameValuePair;
import org.mule.module.oauth2.internal.OAuthConstants;
import org.mule.module.oauth2.internal.StateEncoder;
import org.mule.module.oauth2.internal.TokenResponseProcessor;
import org.mule.module.oauth2.internal.authorizationcode.state.UserOAuthContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the Token request and response handling behaviour of the OAuth 2.0 dance. It provides support for
 * standard OAuth server implementations of the token acquisition part plus a couple of configuration attributes to
 * customize behaviour.
 */
public class AutoAuthorizationCodeTokenRequestHandler extends AbstractAuthorizationCodeTokenRequestHandler
{

    private TokenResponseConfiguration tokenResponseConfiguration = new TokenResponseConfiguration();

    public void setTokenResponseConfiguration(final TokenResponseConfiguration tokenResponseConfiguration)
    {
        this.tokenResponseConfiguration = tokenResponseConfiguration;
    }

    /**
     * Starts the http listener for the redirect url callback. This will create a flow with an endpoint on the
     * provided OAuth redirect uri parameter. The OAuth Server will call this url to provide the authentication code
     * required to get the access token.
     *
     * @throws MuleException if the listener couldn't be created.
     */
    public void init() throws MuleException
    {
        createListenerForRedirectUrl();
    }

    protected MessageProcessor createRedirectUrlProcessor()
    {
        return new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                Map<String, String> queryParams = event.getMessage().getInboundProperty(MessageProperties.HTTP_QUERY_PARAMS);
                String authorizationCode = queryParams.get(OAuthConstants.CODE_PARAMETER);
                String state = queryParams.get(OAuthConstants.STATE_PARAMETER);
                setMapPayloadWithTokenRequestParameters(event, authorizationCode);
                final MuleEvent tokenUrlResposne = invokeTokenUrl(event);
                decodeStateAndUpdateOAuthUserState(tokenUrlResposne, state);
                event.getMessage().setPayload("Successfully retrieved access token!");
                return event;
            }
        };
    }

    private void setMapPayloadWithTokenRequestParameters(final MuleEvent event, final String authorizationCode)
    {
        final HashMap<String, String> formData = new HashMap<String, String>();
        formData.put(OAuthConstants.CODE_PARAMETER, authorizationCode);
        formData.put(OAuthConstants.CLIENT_ID_PARAMETER, getOauthConfig().getClientId());
        formData.put(OAuthConstants.CLIENT_SECRET_PARAMETER, getOauthConfig().getClientSecret());
        formData.put(OAuthConstants.GRANT_TYPE_PARAMETER, OAuthConstants.GRANT_TYPE_AUTHENTICATION_CODE);
        formData.put(OAuthConstants.REDIRECT_URI_PARAMETER, getOauthConfig().getRedirectionUrl());
        event.getMessage().setPayload(formData);
    }

    private void setMapPayloadWithRefreshTokenRequestParameters(final MuleEvent event, final String refreshToken)
    {
        final HashMap<String, String> formData = new HashMap<String, String>();
        formData.put(OAuthConstants.REFRESH_TOKEN_PARAMETER, refreshToken);
        formData.put(OAuthConstants.CLIENT_ID_PARAMETER, getOauthConfig().getClientId());
        formData.put(OAuthConstants.CLIENT_SECRET_PARAMETER, getOauthConfig().getClientSecret());
        formData.put(OAuthConstants.GRANT_TYPE_PARAMETER, OAuthConstants.GRANT_TYPE_REFRESH_TOKEN);
        formData.put(OAuthConstants.REDIRECT_URI_PARAMETER, getOauthConfig().getRedirectionUrl());
        event.getMessage().setPayload(formData);
    }

    private void decodeStateAndUpdateOAuthUserState(final MuleEvent tokenUrlResponse, final String originalState) throws org.mule.api.registry.RegistrationException
    {
        String decodedState = StateEncoder.decodeOriginalState(originalState);
        String encodedResourceOwnerId = StateEncoder.decodeResourceOwnerId(originalState);
        String resourceOwnerId = encodedResourceOwnerId == null ? UserOAuthContext.DEFAULT_RESOURCE_OWNER_ID : encodedResourceOwnerId;

        final UserOAuthContext stateForUser = getOauthConfig().getUserOAuthContext().getContextForUser(resourceOwnerId);

        processTokenUrlResponse(tokenUrlResponse, decodedState, stateForUser);
    }

    private void processTokenUrlResponse(MuleEvent tokenUrlResponse, String decodedState, UserOAuthContext stateForUser)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Update OAuth Context for resourceOwnerId %s", stateForUser.getResourceOwnerId());
        }
        final TokenResponseProcessor tokenResponseProcessor = TokenResponseProcessor.createAuthorizationCodeProcessor(tokenResponseConfiguration, getMuleContext().getExpressionManager());
        tokenResponseProcessor.process(tokenUrlResponse);

        stateForUser.setAccessToken(tokenResponseProcessor.getAccessToken());
        stateForUser.setRefreshToken(tokenResponseProcessor.getRefreshToken());
        stateForUser.setExpiresIn(tokenResponseProcessor.getExpiresIn());

        //State may be null because there's no state or because this was called after refresh token.
        if (decodedState != null)
        {
            stateForUser.setState(decodedState);
        }

        for (NameValuePair parameter : tokenResponseProcessor.getCustomResponseParameters())
        {
            if (parameter.getValue() != null)
            {
                stateForUser.getTokenResponseParameters().put(parameter.getName(), parameter.getValue());
            }
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("New OAuth State for resourceOwnerId %s is: accessToken(%s), refreshToken(%s), expiresIn(%s), state(%s)", stateForUser.getResourceOwnerId(), stateForUser.getAccessToken(), stateForUser.getRefreshToken(), stateForUser.getExpiresIn(), stateForUser.getState());
        }
    }

    /**
     * Executes a refresh token for a particular user. It will call the OAuth Server token url
     * and provide the refresh token to get a new access token.
     *
     * @param currentEvent the event being processed when the refresh token was required.
     * @param userOAuthContext oauth context for who we need to update the access token.
     */
    public void doRefreshToken(final MuleEvent currentEvent, final UserOAuthContext userOAuthContext)
    {
        try
        {
            final MuleEvent muleEvent = DefaultMuleEvent.copy(currentEvent);
            muleEvent.getMessage().clearProperties(PropertyScope.OUTBOUND);
            final String userRefreshToken = userOAuthContext.getRefreshToken();
            if (userRefreshToken == null)
            {
                throw new DefaultMuleException(CoreMessages.createStaticMessage("The user with user id %s has no refresh token in his OAuth state so we can't execute the refresh token call", userOAuthContext.getResourceOwnerId()));
            }
            setMapPayloadWithRefreshTokenRequestParameters(muleEvent, userRefreshToken);
            final MuleEvent refreshTokenResponse = invokeTokenUrl(muleEvent);

            processTokenUrlResponse(refreshTokenResponse, null, userOAuthContext);
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
    }
}
