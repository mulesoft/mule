/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal;

import org.mule.DefaultMuleEvent;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.registry.RegistrationException;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.http.HttpRequester;
import org.mule.module.http.listener.MessageProperties;
import org.mule.module.http.request.HttpRequesterBuilder;
import org.mule.module.oauth2.internal.state.UserOAuthState;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the Token request and response handling behaviour of the OAuth 2.0 dance. It provides support for
 * standard OAuth server implementations of the token acquisition part plus a couple of configuration attributes to
 * customize behaviour.
 */
public class AutoTokenRequestHandler extends AbstractTokenRequestHandler
{

    private String tokenUrl;
    private TokenResponseConfiguration tokenResponseConfiguration = new TokenResponseConfiguration();
    private HttpRequester httpRequester;

    public void setTokenUrl(final String tokenUrl)
    {
        this.tokenUrl = tokenUrl;
    }

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
        httpRequester = new HttpRequesterBuilder(getMuleContext())
                .setAddress(tokenUrl)
                .setConfig(getOauthConfig().getRequestConfig())
                .setMethod("POST")
                .build();
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

    private MuleEvent invokeTokenUrl(final MuleEvent event) throws MuleException
    {
        return httpRequester.process(event);
    }

    private void decodeStateAndUpdateOAuthUserState(final MuleEvent tokenUrlResponse, final String originalState) throws org.mule.api.registry.RegistrationException
    {
        String decodedState = StateEncoder.decodeOriginalState(originalState);
        String encodedOauthStateId = StateEncoder.decodeOAuthStateId(originalState);
        String oauthStateId = encodedOauthStateId == null ? UserOAuthState.DEFAULT_USER_ID : encodedOauthStateId;
        updateOAuthUserState(tokenUrlResponse, decodedState, getOauthConfig().getOAuthState().getStateForUser(oauthStateId));
    }

    private void updateOAuthUserState(final MuleEvent tokenUrlResponse, final String state, final UserOAuthState userOAuthState) throws RegistrationException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Update OAuth State for oauthStateId %s", userOAuthState.getUserId());
        }
        final ExpressionManager expressionManager = getMuleContext().getExpressionManager();
        final String accessToken = expressionManager.parse(tokenResponseConfiguration.getAccessToken(), tokenUrlResponse);
        final String refreshToken = expressionManager.parse(tokenResponseConfiguration.getRefreshToken(), tokenUrlResponse);
        final String expiresIn = expressionManager.parse(tokenResponseConfiguration.getExpiresIn(), tokenUrlResponse);

        if (logger.isDebugEnabled())
        {
            logger.debug("New OAuth State for oauthStateId %s is: accessToken(%s), refreshToken(%s), expiresIn(%s), state(%s)", userOAuthState.getUserId(), accessToken, refreshToken, expiresIn, state);
        }

        userOAuthState.setAccessToken(accessToken);
        userOAuthState.setRefreshToken(refreshToken);
        userOAuthState.setExpiresIn(expiresIn);

        //State may be null because there's no state or because this was called after refresh token.
        if (state != null)
        {
            userOAuthState.setState(state);
        }

        for (ParameterExtractor parameterExtractor : tokenResponseConfiguration.getParameterExtractors())
        {
            final Object parameterValue = expressionManager.evaluate(parameterExtractor.getValue(), tokenUrlResponse);
            if (parameterValue != null)
            {
                userOAuthState.getTokenResponseParameters().put(parameterExtractor.getParamName(), parameterValue);
            }
        }
    }

    /**
     * Executes a refresh token for a particular user. It will call the OAuth Server token url
     * and provide the refresh token to get a new access token.
     *
     * @param currentEvent the event being processed when the refresh token was required.
     * @param userOAuthState oauth state for who we need to update the access token.
     */
    public void doRefreshToken(final MuleEvent currentEvent, final UserOAuthState userOAuthState)
    {
        try
        {
            final MuleEvent muleEvent = DefaultMuleEvent.copy(currentEvent);
            muleEvent.getMessage().clearProperties(PropertyScope.OUTBOUND);
            final String userRefreshToken = userOAuthState.getRefreshToken();
            if (userRefreshToken == null)
            {
                throw new DefaultMuleException(CoreMessages.createStaticMessage("The user with user id %s has no refresh token in his OAuth state so we can't execute the refresh token call", userOAuthState.getUserId()));
            }
            setMapPayloadWithRefreshTokenRequestParameters(muleEvent, userRefreshToken);
            final MuleEvent refreshTokenResponse = invokeTokenUrl(muleEvent);
            updateOAuthUserState(refreshTokenResponse, null, userOAuthState);
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
    }
}
