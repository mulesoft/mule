/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal;

import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.module.oauth2.internal.AuthenticationCodeAuthenticate.buildAuthorizationHeaderContent;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.http.HttpHeaders;
import org.mule.module.oauth2.api.RequestAuthenticationException;
import org.mule.module.oauth2.internal.state.UserOAuthState;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

@SmallTest
public class AuthenticationCodeAuthenticateTestCase extends AbstractMuleTestCase
{

    private static final String ACCESS_TOKEN = "!23456789";
    public static final String CUSTOM_OAUTH_STATE_ID = "pepe";

    private MuleContext mockMuleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS.get());
    private AuthorizationCodeConfig mockAuthorizationCodeConfig = mock(AuthorizationCodeConfig.class, RETURNS_DEEP_STUBS.get());
    private MuleEvent mockMuleEvent = Mockito.mock(MuleEvent.class, RETURNS_DEEP_STUBS.get());
    private UserOAuthState mockUserOAuthState = Mockito.mock(UserOAuthState.class, RETURNS_DEEP_STUBS.get());
    private ExpressionManager mockExpressionManager = Mockito.mock(ExpressionManager.class, RETURNS_DEEP_STUBS.get());
    private AuthenticationCodeAuthenticate authenticationCodeAuthenticate = new AuthenticationCodeAuthenticate();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void processWithoutUserIdConfigured() throws Exception
    {
        configureAuthenticationCodeAuthenticate();

        when(mockAuthorizationCodeConfig.getOAuthState().getStateForUser(UserOAuthState.DEFAULT_USER_ID)).thenReturn(mockUserOAuthState);
        when(mockUserOAuthState.getAccessToken()).thenReturn(ACCESS_TOKEN);
        authenticationCodeAuthenticate.authenticate(mockMuleEvent);

        verify(mockMuleEvent.getMessage()).setOutboundProperty(HttpHeaders.Names.AUTHORIZATION, buildAuthorizationHeaderContent(ACCESS_TOKEN));
    }

    @Test
    public void processWithoutUserIdConfiguredAndNoAccessToken() throws Exception
    {
        configureAuthenticationCodeAuthenticate();

        when(mockAuthorizationCodeConfig.getOAuthState().getStateForUser(UserOAuthState.DEFAULT_USER_ID)).thenReturn(mockUserOAuthState);
        when(mockUserOAuthState.getAccessToken()).thenReturn(null);

        expectedException.expect(RequestAuthenticationException.class);
        authenticationCodeAuthenticate.authenticate(mockMuleEvent);
    }

    @Test
    public void processWithNullOAuthStateIdExpression() throws Exception
    {
        authenticationCodeAuthenticate.setOauthStateId("#[null]");
        configureAuthenticationCodeAuthenticate();

        when(mockExpressionManager.evaluate(anyString(), any(MuleEvent.class))).thenReturn(null);

        when(mockAuthorizationCodeConfig.getOAuthState().getStateForUser(UserOAuthState.DEFAULT_USER_ID)).thenReturn(mockUserOAuthState);
        when(mockUserOAuthState.getAccessToken()).thenReturn(null);

        expectedException.expect(RequestAuthenticationException.class);
        authenticationCodeAuthenticate.authenticate(mockMuleEvent);
    }

    @Test
    public void shouldRetryRequest() throws Exception
    {
        shouldRetryRequestForUser(null);
    }

    @Test
    public void shouldRetryRequestCustomOAuthStateId() throws Exception
    {
        shouldRetryRequestForUser(CUSTOM_OAUTH_STATE_ID);
    }

    @Test
    public void shouldNotRetryRequest() throws Exception
    {
        when(mockExpressionManager.evaluate(anyString(), any(MuleEvent.class))).thenReturn(false);
        when(mockAuthorizationCodeConfig.getRefreshTokenWhen()).thenReturn("someValue");
        configureAuthenticationCodeAuthenticate();
        assertThat(authenticationCodeAuthenticate.shouldRetry(mockMuleEvent), Is.is(false));
    }

    @Test
    public void shouldRetryRequestWithNullRefreshTokenWhen() throws Exception
    {
        when(mockAuthorizationCodeConfig.getRefreshTokenWhen()).thenReturn(null);
        configureAuthenticationCodeAuthenticate();
        assertThat(authenticationCodeAuthenticate.shouldRetry(mockMuleEvent), Is.is(false));
    }

    private void shouldRetryRequestForUser(String oauthStateId) throws Exception
    {
        when(mockExpressionManager.evaluate(anyString(), any(MuleEvent.class))).thenReturn(true);
        if (oauthStateId != null)
        {
            authenticationCodeAuthenticate.setOauthStateId(oauthStateId);
        }
        when(mockAuthorizationCodeConfig.getRefreshTokenWhen()).thenReturn("someValue");
        configureAuthenticationCodeAuthenticate();
        assertThat(authenticationCodeAuthenticate.shouldRetry(mockMuleEvent), Is.is(true));
        verify(mockAuthorizationCodeConfig).refreshToken(mockMuleEvent, oauthStateId == null ? UserOAuthState.DEFAULT_USER_ID : oauthStateId);
    }

    private void configureAuthenticationCodeAuthenticate() throws org.mule.api.registry.RegistrationException, InitialisationException
    {
        authenticationCodeAuthenticate.setMuleContext(mockMuleContext);
        when(mockMuleContext.getExpressionManager()).thenReturn(mockExpressionManager);
        authenticationCodeAuthenticate.setConfig(mockAuthorizationCodeConfig);
        authenticationCodeAuthenticate.initialise();
    }
}
