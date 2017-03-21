/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.processor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.module.http.api.HttpConstants.RequestProperties.HTTP_QUERY_PARAMS;
import static org.mule.security.oauth.OAuthProperties.DEFAULT_EVENT_STATE_TEMPLATE;
import static org.mule.security.oauth.notification.OAuthAuthorizeNotification.OAUTH_AUTHORIZATION_END;
import org.mule.RequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.store.ObjectDoesNotExistException;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.transport.PropertyScope;
import org.mule.module.http.internal.ParameterMap;
import org.mule.security.oauth.OAuth2Adapter;
import org.mule.security.oauth.OAuth2Manager;
import org.mule.security.oauth.OAuthProperties;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class OAuth2FetchAccessTokenProcessorTestCase
{

    private static final String verifier = "verifier";
    private static final String eventId = UUID.randomUUID().toString();

    private String state;
    private String incomingState;
    private boolean exception;
    private OAuth2Manager<OAuth2Adapter> manager = null;
    private OAuth2FetchAccessTokenMessageProcessor processor;
    private MuleEvent event;
    private MuleEvent restoredEvent;
    private ParameterMap parameters = new ParameterMap();
    private ParameterMap restoredParameters = new ParameterMap();

    @Mock
    private MuleContext muleContext;

    @Before
    @SuppressWarnings({"unchecked", "deprecation"})
    public void setUp() throws Exception
    {
        state = "my state";
        incomingState = String.format(DEFAULT_EVENT_STATE_TEMPLATE + "%s", eventId, state);
        exception = false;

        restoredEvent = mock(MuleEvent.class, RETURNS_DEEP_STUBS);
        when(restoredEvent.getMessage().getInboundProperty("http.query.params")).thenReturn(restoredParameters);

        manager = mock(OAuth2Manager.class, RETURNS_DEEP_STUBS);
        when(manager.restoreAuthorizationEvent(eventId)).thenReturn(restoredEvent);

        processor = new OAuth2FetchAccessTokenMessageProcessor(manager, null);
        processor.setMuleContext(muleContext);

        event = mock(MuleEvent.class, RETURNS_DEEP_STUBS);
        when(event.getMessage().getInvocationProperty(OAuthProperties.VERIFIER)).thenReturn(verifier);
        parameters.put("state", incomingState);
        when(event.getMessage().getInboundProperty("http.query.params")).thenReturn(parameters);

        when(
                restoredEvent.getMuleContext()
                        .getExpressionManager()
                        .parse(anyString(), any(MuleMessage.class))).thenAnswer(new Answer<String>()
        {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable
            {
                return (String) invocation.getArguments()[0];
            }
        });
    }

    @After
    public void tearDown() throws Exception
    {
        if (!exception)
        {
            verify(manager).restoreAuthorizationEvent(eventId);
            assertSame(RequestContext.getEvent(), restoredEvent);
            verify(muleContext).fireNotification(
                argThat(new OAuthNotificationMatcher(OAUTH_AUTHORIZATION_END, event)));

            verify(restoredEvent.getMessage()).removeProperty(OAuthProperties.HTTP_STATUS,
                PropertyScope.OUTBOUND);
            verify(restoredEvent.getMessage()).removeProperty(OAuthProperties.CALLBACK_LOCATION,
                PropertyScope.OUTBOUND);
        }
    }

    @Test
    public void stateRestored() throws Exception
    {
        final AtomicReference<HashMap<String, String>> parameters = new AtomicReference<>();
        MuleMessage mockMessage = mock(MuleMessage.class);
        when(restoredEvent.getMessage()).thenReturn(mockMessage);
        when(mockMessage.getInboundProperty(HTTP_QUERY_PARAMS)).thenReturn(restoredParameters);
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                if (invocationOnMock.getArguments()[0].equals(HTTP_QUERY_PARAMS))
                {
                    parameters.set((HashMap) invocationOnMock.getArguments()[1]);
                }
                return null;
            }
        }).when(mockMessage).setProperty(anyString(), any(Map.class), any(PropertyScope.class));
        adapterWithUrlUsingConfigAsId();
        assertThat(parameters.get().get("state"), equalTo(state));
    }

    @Test
    public void badState() throws Exception
    {
        incomingState = "bad state";
        exception = true;
        parameters.clear();
        parameters.put("state", incomingState);

        final String accessTokenUrl = "accessTokenUrl";
        final String redirectUri = "redirectUri";
        final String accessTokenId = "accessTokenId";

        processor.setRedirectUri(redirectUri);

        OAuth2Adapter adapter = mock(OAuth2Adapter.class);

        when(manager.createAdapter(restoredEvent, verifier)).thenReturn(adapter);
        when(manager.getDefaultUnauthorizedConnector().getName()).thenReturn(accessTokenId);
        when(adapter.getAccessTokenUrl()).thenReturn(accessTokenUrl);

        assertSame(event, processor.process(event));
    }

    @Test(expected = MessagingException.class)
    public void noAuthorizationEvent() throws Exception
    {
        exception = true;
        when(manager.restoreAuthorizationEvent(eventId)).thenThrow(
            new ObjectDoesNotExistException());
        adapterWithUrlUsingConfigAsId();
    }

    @Test(expected = MessagingException.class)
    public void failToRestoreAuthorizationEvent() throws Exception
    {
        exception = true;
        when(manager.restoreAuthorizationEvent(eventId)).thenThrow(new ObjectStoreException());
        adapterWithUrlUsingConfigAsId();
    }

    @Test
    public void adapterWithUrlUsingConfigAsId() throws Exception
    {
        final String accessTokenUrl = "accessTokenUrl";
        final String redirectUri = "redirectUri";
        final String accessTokenId = "accessTokenId";

        processor.setRedirectUri(redirectUri);

        OAuth2Adapter adapter = mock(OAuth2Adapter.class);

        when(manager.createAdapter(restoredEvent, verifier)).thenReturn(adapter);
        when(manager.getDefaultUnauthorizedConnector().getName()).thenReturn(accessTokenId);
        when(adapter.getAccessTokenUrl()).thenReturn(accessTokenUrl);

        assertSame(restoredEvent, processor.process(event));

        verify(adapter).fetchAccessToken(redirectUri);
        verify(manager.getAccessTokenPoolFactory()).passivateObject(accessTokenId, adapter);
        verify(restoredEvent.getMessage()).setInvocationProperty(
            OAuthProperties.ACCESS_TOKEN_ID, accessTokenId);

    }

    @Test
    public void adapterWitouthUrlUsingConfigAsId() throws Exception
    {
        final String accessTokenUrl = "accessTokenUrl";
        final String redirectUri = "redirectUri";
        final String accessTokenId = "accessTokenId";

        processor.setRedirectUri(redirectUri);
        processor.setAccessTokenUrl(accessTokenUrl);

        OAuth2Adapter adapter = mock(OAuth2Adapter.class);

        when(manager.createAdapter(restoredEvent, verifier)).thenReturn(adapter);
        when(manager.getDefaultUnauthorizedConnector().getName()).thenReturn(accessTokenId);

        assertSame(restoredEvent, processor.process(event));

        verify(adapter).fetchAccessToken(redirectUri);
        verify(manager.getAccessTokenPoolFactory()).passivateObject(accessTokenId, adapter);
        verify(restoredEvent.getMessage()).setInvocationProperty(
            OAuthProperties.ACCESS_TOKEN_ID, accessTokenId);
    }

    @Test
    public void adapterWithUrlUsingCustomId() throws Exception
    {
        final String accessTokenUrl = "accessTokenUrl";
        final String redirectUri = "redirectUri";
        final String accessTokenId = "accessTokenId";

        processor.setRedirectUri(redirectUri);
        processor.setAccessTokenId(accessTokenId);

        OAuth2Adapter adapter = mock(OAuth2Adapter.class);

        when(manager.createAdapter(restoredEvent, verifier)).thenReturn(adapter);
        when(adapter.getAccessTokenUrl()).thenReturn(accessTokenUrl);

        assertSame(restoredEvent, processor.process(event));

        verify(adapter).fetchAccessToken(redirectUri);
        verify(manager.getAccessTokenPoolFactory()).passivateObject(accessTokenId, adapter);
        verify(restoredEvent.getMessage()).setInvocationProperty(
            OAuthProperties.ACCESS_TOKEN_ID, accessTokenId);

    }

    @Test
    public void adapterWitouthUrlUsingCustomId() throws Exception
    {
        final String accessTokenUrl = "accessTokenUrl";
        final String redirectUri = "redirectUri";
        final String accessTokenId = "accessTokenId";

        processor.setRedirectUri(redirectUri);
        processor.setAccessTokenUrl(accessTokenUrl);
        processor.setAccessTokenId(accessTokenId);

        OAuth2Adapter adapter = mock(OAuth2Adapter.class);

        when(manager.createAdapter(restoredEvent, verifier)).thenReturn(adapter);

        assertSame(restoredEvent, processor.process(event));

        verify(adapter).fetchAccessToken(redirectUri);
        verify(manager.getAccessTokenPoolFactory()).passivateObject(accessTokenId, adapter);
        verify(restoredEvent.getMessage()).setInvocationProperty(
            OAuthProperties.ACCESS_TOKEN_ID, accessTokenId);
    }

}
