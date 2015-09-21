/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.http.internal.domain.request.HttpRequestContext;
import org.mule.module.http.internal.listener.async.HttpResponseReadyCallback;
import org.mule.module.http.internal.listener.async.RequestHandler;
import org.mule.module.http.internal.listener.matcher.ListenerRequestMatcher;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

@SmallTest
public class HttpListenerTestCase extends AbstractMuleTestCase
{

    public static final String URI_PARAM_NAME = "uri-param-name";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private MuleContext mockMuleContext = mock(MuleContext.class, Answers.RETURNS_DEEP_STUBS.get());
    private FlowConstruct mockFlowConstruct = mock(FlowConstruct.class, Answers.RETURNS_DEEP_STUBS.get());
    private DefaultHttpListenerConfig mockHttpListenerConfig = mock(DefaultHttpListenerConfig.class, Answers.RETURNS_DEEP_STUBS.get());
    private HttpListenerConnectionManager mockHttpListenerConnectionManager = mock(HttpListenerConnectionManager.class, Answers.RETURNS_DEEP_STUBS.get());

    @Test
    public void cannotHaveTwoUriParamsWithSameName() throws Exception
    {
        final String listenerPath = String.format("/{%s}/{%s}", URI_PARAM_NAME, URI_PARAM_NAME);
        useInvalidPath(listenerPath);
    }

    @Test
    public void cannotHaveWildcardWithOtherCharacters() throws Exception
    {
        useInvalidPath("/path/*pepe");
    }

    @Test
    public void eventCreation() throws Exception
    {
    	final AtomicReference<RequestHandler> requestHandlerRef = new AtomicReference<>();
    	when(mockHttpListenerConfig.addRequestHandler(any(ListenerRequestMatcher.class), any(RequestHandler.class))).then(new Answer<RequestHandlerManager>() {
    		@Override
    		public RequestHandlerManager answer(InvocationOnMock invocation) throws Throwable {
    			requestHandlerRef.set((RequestHandler) invocation.getArguments()[1]);
    			return null;
    		}
		});
    	useInvalidPath("/");

    	assertThat(RequestContext.getEvent(), is(nullValue()));
    	requestHandlerRef.get().handleRequest(mock(HttpRequestContext.class), mock(HttpResponseReadyCallback.class));
    	
    	assertThat(RequestContext.getEvent(), not(nullValue()));
    }
    
    private void useInvalidPath(String listenerPath) throws InitialisationException
    {

        final DefaultHttpListener httpListener = new DefaultHttpListener();
        httpListener.setMuleContext(mockMuleContext);
        httpListener.setFlowConstruct(mockFlowConstruct);
        httpListener.setConfig(mockHttpListenerConfig);
        when(mockHttpListenerConfig.getFullListenerPath(anyString())).thenReturn(new ListenerPath(null ,listenerPath));
        when(mockMuleContext.getRegistry().get(HttpListenerConnectionManager.HTTP_LISTENER_CONNECTION_MANAGER)).thenReturn(mockHttpListenerConnectionManager);
        httpListener.setPath(listenerPath);

        expectedException.expect(InitialisationException.class);
        httpListener.initialise();
    }

}
