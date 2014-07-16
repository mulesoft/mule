/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.processor;

import org.mule.RequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.store.ObjectDoesNotExistException;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.transport.PropertyScope;
import org.mule.security.oauth.OAuth2Adapter;
import org.mule.security.oauth.OAuth2Manager;
import org.mule.security.oauth.OAuthProperties;
import org.mule.security.oauth.notification.OAuthAuthorizeNotification;
import org.mule.tck.size.SmallTest;

import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
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

    @Mock
    private MuleContext muleContext;

    @Before
    @SuppressWarnings({"unchecked", "deprecation"})
    public void setUp() throws Exception
    {
        this.state = "my state";
        this.incomingState = String.format(OAuthProperties.EVENT_STATE_TEMPLATE + "%s", eventId, state);
        this.exception = false;

        this.restoredEvent = Mockito.mock(MuleEvent.class, Mockito.RETURNS_DEEP_STUBS);

        this.manager = Mockito.mock(OAuth2Manager.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(this.manager.restoreAuthorizationEvent(eventId)).thenReturn(restoredEvent);

        this.processor = new OAuth2FetchAccessTokenMessageProcessor(this.manager, null);
        this.processor.setMuleContext(this.muleContext);

        this.event = Mockito.mock(MuleEvent.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(event.getMessage().getInvocationProperty(OAuthProperties.VERIFIER)).thenReturn(verifier);
        Mockito.when(event.getMessage().getInboundProperty("state")).thenReturn(incomingState);

        Mockito.when(
            this.restoredEvent.getMuleContext()
                .getExpressionManager()
                .parse(Mockito.anyString(), Mockito.any(MuleMessage.class))).thenAnswer(new Answer<String>()
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
            Mockito.verify(this.manager).restoreAuthorizationEvent(eventId);
            Assert.assertSame(RequestContext.getEvent(), this.restoredEvent);
            Mockito.verify(this.muleContext).fireNotification(
                Mockito.argThat(new OAuthNotificationMatcher(
                    OAuthAuthorizeNotification.OAUTH_AUTHORIZATION_END, this.event)));

            Mockito.verify(this.restoredEvent.getMessage()).removeProperty(OAuthProperties.HTTP_STATUS,
                PropertyScope.OUTBOUND);
            Mockito.verify(this.restoredEvent.getMessage()).removeProperty(OAuthProperties.CALLBACK_LOCATION,
                PropertyScope.OUTBOUND);
        }
    }

    @Test
    public void stateRestored() throws Exception
    {
        this.adapterWithUrlUsingConfigAsId();
        Mockito.verify(this.restoredEvent.getMessage()).setProperty("state", state, PropertyScope.INBOUND);
    }

    @Test
    public void badState() throws Exception
    {
        this.incomingState = "bad state";
        this.exception = true;

        Mockito.when(event.getMessage().getInboundProperty("state")).thenReturn(incomingState);
        final String accessTokenUrl = "accessTokenUrl";
        final String redirectUri = "redirectUri";
        final String accessTokenId = "accessTokenId";

        this.processor.setRedirectUri(redirectUri);

        OAuth2Adapter adapter = Mockito.mock(OAuth2Adapter.class);

        Mockito.when(this.manager.createAdapter(verifier)).thenReturn(adapter);
        Mockito.when(this.manager.getDefaultUnauthorizedConnector().getName()).thenReturn(accessTokenId);
        Mockito.when(adapter.getAccessTokenUrl()).thenReturn(accessTokenUrl);

        Assert.assertSame(this.event, this.processor.process(this.event));
    }

    @Test(expected = MessagingException.class)
    public void noAuthorizationEvent() throws Exception
    {
        this.exception = true;
        Mockito.when(this.manager.restoreAuthorizationEvent(eventId)).thenThrow(
            new ObjectDoesNotExistException());
        this.adapterWithUrlUsingConfigAsId();
    }

    @Test(expected = MessagingException.class)
    public void failToRestoreAuthorizationEvent() throws Exception
    {
        this.exception = true;
        Mockito.when(this.manager.restoreAuthorizationEvent(eventId)).thenThrow(new ObjectStoreException());
        this.adapterWithUrlUsingConfigAsId();
    }

    @Test
    public void adapterWithUrlUsingConfigAsId() throws Exception
    {
        final String accessTokenUrl = "accessTokenUrl";
        final String redirectUri = "redirectUri";
        final String accessTokenId = "accessTokenId";

        this.processor.setRedirectUri(redirectUri);

        OAuth2Adapter adapter = Mockito.mock(OAuth2Adapter.class);

        Mockito.when(this.manager.createAdapter(verifier)).thenReturn(adapter);
        Mockito.when(this.manager.getDefaultUnauthorizedConnector().getName()).thenReturn(accessTokenId);
        Mockito.when(adapter.getAccessTokenUrl()).thenReturn(accessTokenUrl);

        Assert.assertSame(this.restoredEvent, this.processor.process(event));

        Mockito.verify(adapter).fetchAccessToken(redirectUri);
        Mockito.verify(this.manager.getAccessTokenPoolFactory()).passivateObject(accessTokenId, adapter);
        Mockito.verify(this.restoredEvent.getMessage()).setInvocationProperty(
            OAuthProperties.ACCESS_TOKEN_ID, accessTokenId);

    }

    @Test
    public void adapterWitouthUrlUsingConfigAsId() throws Exception
    {
        final String accessTokenUrl = "accessTokenUrl";
        final String redirectUri = "redirectUri";
        final String accessTokenId = "accessTokenId";

        this.processor.setRedirectUri(redirectUri);
        this.processor.setAccessTokenUrl(accessTokenUrl);

        OAuth2Adapter adapter = Mockito.mock(OAuth2Adapter.class);

        Mockito.when(this.manager.createAdapter(verifier)).thenReturn(adapter);
        Mockito.when(this.manager.getDefaultUnauthorizedConnector().getName()).thenReturn(accessTokenId);

        Assert.assertSame(this.restoredEvent, this.processor.process(event));

        Mockito.verify(adapter).fetchAccessToken(redirectUri);
        Mockito.verify(this.manager.getAccessTokenPoolFactory()).passivateObject(accessTokenId, adapter);
        Mockito.verify(this.restoredEvent.getMessage()).setInvocationProperty(
            OAuthProperties.ACCESS_TOKEN_ID, accessTokenId);
    }

    @Test
    public void adapterWithUrlUsingCustomId() throws Exception
    {
        final String accessTokenUrl = "accessTokenUrl";
        final String redirectUri = "redirectUri";
        final String accessTokenId = "accessTokenId";

        this.processor.setRedirectUri(redirectUri);
        this.processor.setAccessTokenId(accessTokenId);

        OAuth2Adapter adapter = Mockito.mock(OAuth2Adapter.class);

        Mockito.when(this.manager.createAdapter(verifier)).thenReturn(adapter);
        Mockito.when(adapter.getAccessTokenUrl()).thenReturn(accessTokenUrl);

        Assert.assertSame(this.restoredEvent, this.processor.process(event));

        Mockito.verify(adapter).fetchAccessToken(redirectUri);
        Mockito.verify(this.manager.getAccessTokenPoolFactory()).passivateObject(accessTokenId, adapter);
        Mockito.verify(this.restoredEvent.getMessage()).setInvocationProperty(
            OAuthProperties.ACCESS_TOKEN_ID, accessTokenId);

    }

    @Test
    public void adapterWitouthUrlUsingCustomId() throws Exception
    {
        final String accessTokenUrl = "accessTokenUrl";
        final String redirectUri = "redirectUri";
        final String accessTokenId = "accessTokenId";

        this.processor.setRedirectUri(redirectUri);
        this.processor.setAccessTokenUrl(accessTokenUrl);
        this.processor.setAccessTokenId(accessTokenId);

        OAuth2Adapter adapter = Mockito.mock(OAuth2Adapter.class);

        Mockito.when(this.manager.createAdapter(verifier)).thenReturn(adapter);

        Assert.assertSame(this.restoredEvent, this.processor.process(event));

        Mockito.verify(adapter).fetchAccessToken(redirectUri);
        Mockito.verify(this.manager.getAccessTokenPoolFactory()).passivateObject(accessTokenId, adapter);
        Mockito.verify(this.restoredEvent.getMessage()).setInvocationProperty(
            OAuthProperties.ACCESS_TOKEN_ID, accessTokenId);
    }

}
