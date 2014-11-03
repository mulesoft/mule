/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.authorizationcode;

import static org.mockito.Mockito.when;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.Mockito;

@SmallTest
public class StoreAuthenticationCodeStateMessageProcessorTestCase extends AbstractMuleTestCase
{

    public static final String ACCESS_TOKEN_RETRIEVE_EXPRESSION = "access_token_retrieve_expression";
    public static final String OAUTH_STATE_ID_EXPRESSION = "#[oauth-state-id-expression]";
    public static final String ACCESS_TOKEN_EXPRESSION = "#[access-token-expression]";
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private MuleContext mockMuleContext = Mockito.mock(MuleContext.class, Answers.RETURNS_DEEP_STUBS.get());
    private AuthorizationCodeGrantType mockOauthConfig = Mockito.mock(AuthorizationCodeGrantType.class, Answers.RETURNS_DEEP_STUBS.get());
    private MuleEvent mockMuleEvent = Mockito.mock(MuleEvent.class, Answers.RETURNS_DEEP_STUBS.get());

    @Test
    public void failIfNoAccessTokenIsSet() throws InitialisationException
    {
        final StoreAuthenticationCodeStateMessageProcessor messageProcessor = createStoreMessageProcessor();
        messageProcessor.setConfig(mockOauthConfig);

        expectedException.expect(InitialisationException.class);
        messageProcessor.initialise();
    }

    @Test
    public void failIfNoOAuthConfigIsSet() throws InitialisationException
    {
        final StoreAuthenticationCodeStateMessageProcessor messageProcessor = createStoreMessageProcessor();
        messageProcessor.setAccessToken(ACCESS_TOKEN_RETRIEVE_EXPRESSION);

        expectedException.expect(InitialisationException.class);
        messageProcessor.initialise();
    }

    @Test
    public void failIfResourceOwnerIdReturnsNull() throws Exception
    {
        final StoreAuthenticationCodeStateMessageProcessor messageProcessor = createValidStoreMessageProcessor();
        when(mockMuleContext.getExpressionManager().evaluate(OAUTH_STATE_ID_EXPRESSION, mockMuleEvent)).thenReturn(null);
        messageProcessor.setResourceOwnerId(OAUTH_STATE_ID_EXPRESSION);
        messageProcessor.initialise();

        expectedException.expect(MuleException.class);
        messageProcessor.process(mockMuleEvent);
    }

    @Test
    public void failIfAccessTokenReturnsNull() throws Exception
    {
        final StoreAuthenticationCodeStateMessageProcessor messageProcessor = createValidStoreMessageProcessor();
        when(mockMuleContext.getExpressionManager().evaluate(ACCESS_TOKEN_EXPRESSION, mockMuleEvent)).thenReturn(null);
        messageProcessor.setAccessToken(ACCESS_TOKEN_EXPRESSION);
        messageProcessor.initialise();

        expectedException.expect(MuleException.class);
        messageProcessor.process(mockMuleEvent);
    }

    private StoreAuthenticationCodeStateMessageProcessor createValidStoreMessageProcessor()
    {
        final StoreAuthenticationCodeStateMessageProcessor storeMessageProcessor = createStoreMessageProcessor();
        storeMessageProcessor.setConfig(mockOauthConfig);
        storeMessageProcessor.setAccessToken(ACCESS_TOKEN_RETRIEVE_EXPRESSION);
        return storeMessageProcessor;
    }

    private StoreAuthenticationCodeStateMessageProcessor createStoreMessageProcessor()
    {
        final StoreAuthenticationCodeStateMessageProcessor messageProcessor = new StoreAuthenticationCodeStateMessageProcessor();
        messageProcessor.setMuleContext(mockMuleContext);
        return messageProcessor;
    }

}
