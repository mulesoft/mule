/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.auth.AuthScope;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.security.Credentials;
import org.mule.api.transport.Connector;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.security.MuleCredentials;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HttpConnectorClientAuthorizationTestCase extends AbstractMuleContextTestCase
{
    private static final String CREDENTIALS_USER = "myUser";

    private static final String CREDENTIALS_PASSWORD = "myPassword";

    private static final String URI_WITHOUT_CREDENTIALS = "http://localhost:60127";

    private static final String URI_WITH_CREDENTIALS = "http://" + CREDENTIALS_USER + ":" + CREDENTIALS_PASSWORD + "@localhost:60127";

    private static final String HEADER_AUTHORIZATION_VALUE = "headerAuthValue";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MuleEvent mockMuleEvent;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpClient mockHttpClient;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpMethod mockHttpMethod;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ImmutableEndpoint mockImmutableEndpoint;

    private URI uri;

    private MuleMessage message;

    private String encoding;

    private HttpConnector connector;

    @Before
    public void setup() throws URISyntaxException {
        uri = new URI(URI_WITHOUT_CREDENTIALS);
        message = new DefaultMuleMessage(StringUtils.EMPTY, muleContext);
        encoding = muleContext.getConfiguration().getDefaultEncoding();
        connector = new HttpConnector(muleContext);
        connector.setName("test");
    }

    @Test
    public void testWithCredentials() throws Exception
    {
        Credentials credentials = new MuleCredentials(CREDENTIALS_USER, CREDENTIALS_PASSWORD.toCharArray());

        when(mockMuleEvent.getCredentials()).thenReturn(credentials);
        when(mockMuleEvent.getMessageSourceURI()).thenReturn(uri);
        when(mockMuleEvent.getMessage()).thenReturn(message);

        connector.setupClientAuthorization(mockMuleEvent, mockHttpMethod, mockHttpClient, mockImmutableEndpoint);

        verify(mockHttpClient.getState(), atLeast(1)).setCredentials(isA(AuthScope.class), isA(org.apache.commons.httpclient.Credentials.class));
    }

    @Test
    public void testWithUserInfo() throws Exception
    {
        URI uri = new URI(URI_WITH_CREDENTIALS);

        when(mockMuleEvent.getMessageSourceURI()).thenReturn(uri);
        when(mockMuleEvent.getMessage()).thenReturn(message);
        when(mockMuleEvent.getCredentials()).thenReturn(null);
        when(mockImmutableEndpoint.getProperty(HttpConstants.HEADER_AUTHORIZATION)).thenReturn(null);
        when(mockImmutableEndpoint.getEncoding()).thenReturn(encoding);
        when(mockImmutableEndpoint.getEndpointURI()).thenReturn(new MuleEndpointURI(URI_WITH_CREDENTIALS, muleContext));

        connector.setupClientAuthorization(mockMuleEvent, mockHttpMethod, mockHttpClient, mockImmutableEndpoint);

        verify(mockHttpMethod, atLeast(1)).addRequestHeader(eq(HttpConstants.HEADER_AUTHORIZATION), anyString());
    }

    @Test
    public void testWithAuthorizationHeader() throws Exception
    {
        message.setOutboundProperty(HttpConstants.HEADER_AUTHORIZATION, HEADER_AUTHORIZATION_VALUE);

        when(mockMuleEvent.getMessageSourceURI()).thenReturn(uri);
        when(mockMuleEvent.getMessage()).thenReturn(message);
        when(mockMuleEvent.getCredentials()).thenReturn(null);
        when(mockImmutableEndpoint.getProperty(HttpConstants.HEADER_AUTHORIZATION)).thenReturn(HEADER_AUTHORIZATION_VALUE);
        when(mockHttpMethod.getRequestHeader(HttpConstants.HEADER_AUTHORIZATION)).thenReturn(null);

        connector.setupClientAuthorization(mockMuleEvent, mockHttpMethod, mockHttpClient, mockImmutableEndpoint);

        verify(mockHttpMethod, atLeast(1)).addRequestHeader(eq(HttpConstants.HEADER_AUTHORIZATION), anyString());
    }

    @Test
    public void testWithProxyAuth() throws Exception
    {
        when(mockMuleEvent.getMessageSourceURI()).thenReturn(uri);
        when(mockMuleEvent.getMessage()).thenReturn(message);
        when(mockMuleEvent.getCredentials()).thenReturn(null);

        connector.setProxyUsername(CREDENTIALS_USER);
        connector.setProxyPassword(CREDENTIALS_PASSWORD);
        connector.setupClientAuthorization(mockMuleEvent, mockHttpMethod, mockHttpClient, mockImmutableEndpoint);

        verify(mockHttpClient.getParams(), never()).setAuthenticationPreemptive(false);
    }

    @Test
    public void testClean() throws Exception
    {
        when(mockMuleEvent.getMessageSourceURI()).thenReturn(uri);
        when(mockMuleEvent.getMessage()).thenReturn(message);
        when(mockMuleEvent.getCredentials()).thenReturn(null);

        connector.setProxyUsername(null);
        connector.setProxyPassword(null);
        connector.setupClientAuthorization(mockMuleEvent, mockHttpMethod, mockHttpClient, mockImmutableEndpoint);

        verify(mockHttpClient.getParams(), atLeast(1)).setAuthenticationPreemptive(false);
    }

}
