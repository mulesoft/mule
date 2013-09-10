/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.process;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.devkit.ProcessInterceptor;
import org.mule.api.processor.MessageProcessor;
import org.mule.common.connection.exception.UnableToAcquireConnectionException;
import org.mule.devkit.processor.DevkitBasedMessageProcessor;
import org.mule.security.oauth.OAuth2Adapter;
import org.mule.security.oauth.OAuth2Manager;
import org.mule.security.oauth.callback.ProcessCallback;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.List;

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
public class ManagedAccessTokenProcessInterceptorTestCase
{

    private static final String ACCESS_TOKEN_ID = "accessTokenId";

    @Mock
    private ProcessInterceptor<Object, OAuth2Adapter> next;

    private OAuth2Manager<OAuth2Adapter> manager;

    @Mock
    private ProcessCallback<Object, OAuth2Adapter> callback;

    @Mock(extraInterfaces = MessageProcessor.class)
    private DevkitBasedMessageProcessor processor;

    @Mock
    private OAuth2Adapter adapter;

    private MuleEvent event;

    private ManagedAccessTokenProcessInterceptor<Object> interceptor;

    @Before
    @SuppressWarnings({"unchecked", "deprecation"})
    public void setUp() throws Exception
    {
        this.manager = Mockito.mock(OAuth2Manager.class, Mockito.RETURNS_DEEP_STUBS);
        this.interceptor = new ManagedAccessTokenProcessInterceptor<Object>(this.next, this.manager);
        this.event = Mockito.mock(MuleEvent.class, Mockito.RETURNS_DEEP_STUBS);

        Mockito.when(
            event.getMuleContext()
                .getExpressionManager()
                .parse(Mockito.anyString(), Mockito.any(MuleMessage.class))).thenAnswer(new Answer<String>()
        {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable
            {
                return (String) invocation.getArguments()[0];
            }
        });

        List<Class<? extends Exception>> managedExceptions = new ArrayList<Class<? extends Exception>>();
        Mockito.when(this.callback.getManagedExceptions()).thenReturn(managedExceptions);
    }

    @Test
    public void unprotected() throws Exception
    {
        OAuth2Adapter adapter = Mockito.mock(OAuth2Adapter.class);
        Mockito.when(this.manager.getDefaultUnauthorizedConnector()).thenReturn(adapter);
        Mockito.when(this.callback.isProtected()).thenReturn(false);

        this.interceptor.execute(callback, this.adapter, (MessageProcessor) this.processor, this.event);

        Mockito.verify(this.callback).process(Mockito.same(adapter));
        Mockito.verify(this.manager, Mockito.never()).acquireAccessToken(ACCESS_TOKEN_ID);
    }

    @Test
    public void withConfigName() throws Exception
    {
        Mockito.when(this.callback.isProtected()).thenReturn(true);
        Mockito.when(this.manager.getDefaultUnauthorizedConnector().getName()).thenReturn(ACCESS_TOKEN_ID);
        Mockito.when(this.manager.acquireAccessToken(ACCESS_TOKEN_ID)).thenReturn(this.adapter);

        this.interceptor.execute(callback, this.adapter, (MessageProcessor) this.processor, this.event);
        Mockito.verify(this.manager).acquireAccessToken(ACCESS_TOKEN_ID);
        Mockito.verify(this.next).execute(this.callback, this.adapter, (MessageProcessor) this.processor,
            this.event);
    }

    @Test
    public void withDefaultAccessTokenId() throws Exception
    {
        Mockito.when(this.callback.isProtected()).thenReturn(true);
        Mockito.when(this.manager.getDefaultAccessTokenId()).thenReturn(ACCESS_TOKEN_ID);
        Mockito.when(this.manager.acquireAccessToken(ACCESS_TOKEN_ID)).thenReturn(this.adapter);

        this.interceptor.execute(callback, this.adapter, (MessageProcessor) this.processor, this.event);
        Mockito.verify(this.manager).acquireAccessToken(ACCESS_TOKEN_ID);
        Mockito.verify(this.next).execute(this.callback, this.adapter, (MessageProcessor) this.processor,
            this.event);
    }

    @Test
    public void withCustomAccessTokenId() throws Exception
    {
        Mockito.when(this.callback.isProtected()).thenReturn(true);
        Mockito.when(this.processor.getAccessTokenId()).thenReturn(ACCESS_TOKEN_ID);
        Mockito.when(this.manager.acquireAccessToken(ACCESS_TOKEN_ID)).thenReturn(this.adapter);

        this.interceptor.execute(callback, this.adapter, (MessageProcessor) this.processor, this.event);
        Mockito.verify(this.manager).acquireAccessToken(ACCESS_TOKEN_ID);
        Mockito.verify(this.next).execute(this.callback, this.adapter, (MessageProcessor) this.processor,
            this.event);
    }

    @Test(expected = RuntimeException.class)
    public void managedExceptionWithNotNullAdapter() throws Exception
    {
        Mockito.when(this.callback.isProtected()).thenReturn(true);
        Mockito.when(this.processor.getAccessTokenId()).thenReturn(ACCESS_TOKEN_ID);
        Mockito.when(this.manager.acquireAccessToken(ACCESS_TOKEN_ID)).thenReturn(this.adapter);
        Mockito.when(
            this.next.execute(this.callback, this.adapter, (MessageProcessor) this.processor, this.event))
            .thenThrow(new RuntimeException());

        this.interceptor.execute(callback, this.adapter, (MessageProcessor) this.processor, this.event);

        Mockito.verify(this.manager).acquireAccessToken(ACCESS_TOKEN_ID);
        Mockito.verify(this.next, Mockito.never()).execute(this.callback, this.adapter,
            (MessageProcessor) this.processor, this.event);
        Mockito.verify(this.manager).destroyAccessToken(ACCESS_TOKEN_ID, this.adapter);
        Mockito.verify(this.manager).releaseAccessToken(ACCESS_TOKEN_ID, this.adapter);
    }

    @Test(expected = RuntimeException.class)
    public void managedExceptionWithNullAdapter() throws Exception
    {
        Mockito.when(this.callback.isProtected()).thenReturn(true);
        Mockito.when(this.processor.getAccessTokenId()).thenReturn(ACCESS_TOKEN_ID);
        Mockito.when(this.manager.acquireAccessToken(ACCESS_TOKEN_ID)).thenThrow(new RuntimeException());

        this.interceptor.execute(callback, this.adapter, (MessageProcessor) this.processor, this.event);

        Mockito.verify(this.manager).acquireAccessToken(ACCESS_TOKEN_ID);
        Mockito.verify(this.next, Mockito.never()).execute(this.callback, this.adapter,
            (MessageProcessor) this.processor, this.event);
        Mockito.verify(this.manager, Mockito.never()).destroyAccessToken(ACCESS_TOKEN_ID,
            Mockito.any(OAuth2Adapter.class));
        Mockito.verify(this.manager, Mockito.never()).releaseAccessToken(ACCESS_TOKEN_ID,
            Mockito.any(OAuth2Adapter.class));
    }

    @Test(expected = UnableToAcquireConnectionException.class)
    public void noAdapterInObjectStore() throws Exception
    {
        Mockito.when(this.callback.isProtected()).thenReturn(true);
        Mockito.when(this.processor.getAccessTokenId()).thenReturn(ACCESS_TOKEN_ID);
        Mockito.when(this.manager.acquireAccessToken(ACCESS_TOKEN_ID)).thenReturn(null);

        this.interceptor.execute(callback, this.adapter, (MessageProcessor) this.processor, this.event);

        Mockito.verify(this.manager).acquireAccessToken(ACCESS_TOKEN_ID);
        Mockito.verify(this.next, Mockito.never()).execute(this.callback, this.adapter,
            (MessageProcessor) this.processor, this.event);
        Mockito.verify(this.manager, Mockito.never()).destroyAccessToken(ACCESS_TOKEN_ID,
            Mockito.any(OAuth2Adapter.class));
        Mockito.verify(this.manager, Mockito.never()).releaseAccessToken(ACCESS_TOKEN_ID,
            Mockito.any(OAuth2Adapter.class));
    }

}
