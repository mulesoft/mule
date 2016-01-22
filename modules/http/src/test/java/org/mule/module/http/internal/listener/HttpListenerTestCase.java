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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.module.http.api.HttpConstants.HttpStatus.BAD_REQUEST;
import static org.mule.module.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.module.http.api.HttpConstants.Protocols.HTTP;
import static org.mule.module.http.api.HttpHeaders.Names.HOST;
import static org.mule.module.http.internal.domain.HttpProtocol.HTTP_1_0;
import static org.mule.module.http.internal.domain.HttpProtocol.HTTP_1_1;
import static org.mule.module.http.internal.listener.HttpListenerConnectionManager.HTTP_LISTENER_CONNECTION_MANAGER;

import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.registry.RegistrationException;
import org.mule.execution.MessageProcessContext;
import org.mule.execution.MessageProcessingManager;
import org.mule.module.http.internal.domain.ByteArrayHttpEntity;
import org.mule.module.http.internal.domain.HttpProtocol;
import org.mule.module.http.internal.domain.request.ClientConnection;
import org.mule.module.http.internal.domain.request.HttpRequest;
import org.mule.module.http.internal.domain.request.HttpRequestContext;
import org.mule.module.http.internal.domain.response.HttpResponse;
import org.mule.module.http.internal.listener.async.HttpResponseReadyCallback;
import org.mule.module.http.internal.listener.async.RequestHandler;
import org.mule.module.http.internal.listener.async.ResponseStatusCallback;
import org.mule.module.http.internal.listener.matcher.ListenerRequestMatcher;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

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
        usePath("/");

    	assertThat(RequestContext.getEvent(), is(nullValue()));

    	HttpResponseReadyCallback responseCallback = mock(HttpResponseReadyCallback.class);
        doAnswer(new Answer<Void>()
        {
    	    @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
    	    {
    	        assertThat(RequestContext.getEvent(), not(nullValue()));
    	        return null;
    	    }
        }).when(responseCallback).responseReady(any(HttpResponse.class), any(ResponseStatusCallback.class));

        HttpRequest request = buildGetRootRequest(HTTP_1_1);
        when(request.getHeaderValue("host")).thenReturn("localhost");
        HttpRequestContext requestContext = buildRequestContext(request);

        requestHandlerRef.get().handleRequest(requestContext, responseCallback);
    	
        assertThat(RequestContext.getEvent(), is(nullValue()));
    }
    
    @Test
    public void eventCreationWithInvalidPath() throws Exception
    {
        final AtomicReference<RequestHandler> requestHandlerRef = new AtomicReference<>();
        when(mockHttpListenerConfig.addRequestHandler(any(ListenerRequestMatcher.class), any(RequestHandler.class))).then(new Answer<RequestHandlerManager>()
        {
            @Override
            public RequestHandlerManager answer(InvocationOnMock invocation) throws Throwable
            {
                requestHandlerRef.set((RequestHandler) invocation.getArguments()[1]);
                return null;
            }
        });
        useInvalidPath("/");

        assertThat(RequestContext.getEvent(), is(nullValue()));
        requestHandlerRef.get().handleRequest(mock(HttpRequestContext.class), mock(HttpResponseReadyCallback.class));

        assertThat(RequestContext.getEvent(), is(nullValue()));
    }

    /**
     * {@code host} header is not specified in HTTP 1.0
     */
    @Test
    public void noHostHeaderOn10Request() throws Exception
    {
        final AtomicReference<RequestHandler> requestHandlerRef = new AtomicReference<>();
        when(mockHttpListenerConfig.addRequestHandler(any(ListenerRequestMatcher.class), any(RequestHandler.class))).then(new Answer<RequestHandlerManager>()
        {
            @Override
            public RequestHandlerManager answer(InvocationOnMock invocation) throws Throwable
            {
                requestHandlerRef.set((RequestHandler) invocation.getArguments()[1]);
                return null;
            }
        });
        usePath("/");

        HttpRequest request = buildGetRootRequest(HTTP_1_0);
        HttpRequestContext requestContext = buildRequestContext(request);

        HttpResponseReadyCallback responseCallback = mock(HttpResponseReadyCallback.class);
        requestHandlerRef.get().handleRequest(requestContext, responseCallback);
        assertResponse(responseCallback, OK.getStatusCode());
    }

    @Test
    public void noHostHeaderOn11Request() throws Exception
    {
        final AtomicReference<RequestHandler> requestHandlerRef = new AtomicReference<>();
        when(mockHttpListenerConfig.addRequestHandler(any(ListenerRequestMatcher.class), any(RequestHandler.class))).then(new Answer<RequestHandlerManager>()
        {
            @Override
            public RequestHandlerManager answer(InvocationOnMock invocation) throws Throwable
            {
                requestHandlerRef.set((RequestHandler) invocation.getArguments()[1]);
                return null;
            }
        });
        usePath("/");

        HttpRequest request = buildGetRootRequest(HTTP_1_1);
        HttpRequestContext requestContext = buildRequestContext(request);

        HttpResponseReadyCallback responseCallback = mock(HttpResponseReadyCallback.class);
        requestHandlerRef.get().handleRequest(requestContext, responseCallback);
        assertResponse(responseCallback, BAD_REQUEST.getStatusCode(), BAD_REQUEST.getReasonPhrase(), "Missing 'host' header");
    }

    protected HttpRequest buildGetRootRequest(HttpProtocol protocol)
    {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getHeaderValue(HOST)).thenReturn(null);
        when(request.getPath()).thenReturn("/");
        when(request.getUri()).thenReturn("/");
        when(request.getMethod()).thenReturn("GET");
        when(request.getProtocol()).thenReturn(protocol);
        return request;
    }

    protected HttpRequestContext buildRequestContext(HttpRequest request)
    {
        ClientConnection clientConnection = mock(ClientConnection.class);
        when(clientConnection.getRemoteHostAddress()).thenReturn(InetSocketAddress.createUnresolved("localhost", 80));

        HttpRequestContext requestContext = mock(HttpRequestContext.class);
        when(requestContext.getRequest()).thenReturn(request);
        when(requestContext.getClientConnection()).thenReturn(clientConnection);
        when(requestContext.getScheme()).thenReturn(HTTP.getScheme());
        return requestContext;
    }

    protected void assertResponse(HttpResponseReadyCallback responseCallback, int statusCode)
    {
        ArgumentCaptor<HttpResponse> responseCaptor = ArgumentCaptor.<HttpResponse> forClass(HttpResponse.class);
        verify(responseCallback).responseReady(responseCaptor.capture(), any(ResponseStatusCallback.class));
        assertThat(responseCaptor.getValue().getStatusCode(), is(statusCode));
    }

    protected void assertResponse(HttpResponseReadyCallback responseCallback, int statusCode, String reason, String body)
    {
        ArgumentCaptor<HttpResponse> responseCaptor = ArgumentCaptor.<HttpResponse> forClass(HttpResponse.class);
        verify(responseCallback).responseReady(responseCaptor.capture(), any(ResponseStatusCallback.class));
        assertThat(responseCaptor.getValue().getStatusCode(), is(statusCode));
        assertThat(responseCaptor.getValue().getReasonPhrase(), is(reason));
        assertThat(((ByteArrayHttpEntity) responseCaptor.getValue().getEntity()).getContent(), is(body.getBytes()));
    }

    private void useInvalidPath(String listenerPath) throws InitialisationException
    {
        final DefaultHttpListener httpListener = new DefaultHttpListener();
        httpListener.setMuleContext(mockMuleContext);
        httpListener.setFlowConstruct(mockFlowConstruct);
        httpListener.setConfig(mockHttpListenerConfig);
        when(mockHttpListenerConfig.getFullListenerPath(anyString())).thenReturn(new ListenerPath(null ,listenerPath));
        when(mockMuleContext.getRegistry().get(HTTP_LISTENER_CONNECTION_MANAGER)).thenReturn(mockHttpListenerConnectionManager);
        httpListener.setPath(listenerPath);

        expectedException.expect(InitialisationException.class);
        httpListener.initialise();
    }

    private void usePath(String listenerPath) throws InitialisationException, RegistrationException
    {
        MuleContext mockMuleContext = mock(MuleContext.class);
        when(mockMuleContext.getRegistry()).thenReturn(mock(MuleRegistry.class));
        when(mockMuleContext.getConfiguration()).thenReturn(mock(MuleConfiguration.class));
        when(mockMuleContext.getConfiguration().getDefaultEncoding()).thenReturn("UTF-8");

        final DefaultHttpListener httpListener = new DefaultHttpListener();
        httpListener.setMuleContext(mockMuleContext);
        httpListener.setFlowConstruct(mockFlowConstruct);
        httpListener.setConfig(mockHttpListenerConfig);
        when(mockHttpListenerConfig.getFullListenerPath(anyString())).thenReturn(new ListenerPath(null, listenerPath));

        MessageProcessingManager messageProcessingManager = mock(MessageProcessingManager.class);
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                HttpMessageProcessorTemplate template = (HttpMessageProcessorTemplate) invocation.getArguments()[0];
                template.sendResponseToClient(null, null);
                return null;
            }
        }).when(messageProcessingManager).processMessage(any(HttpMessageProcessorTemplate.class), any(MessageProcessContext.class));

        when(mockMuleContext.getRegistry().lookupObject(MessageProcessingManager.class)).thenReturn(messageProcessingManager);
        when(mockMuleContext.getRegistry().get(HTTP_LISTENER_CONNECTION_MANAGER)).thenReturn(mockHttpListenerConnectionManager);
        httpListener.setPath(listenerPath);

        httpListener.initialise();
    }

}
