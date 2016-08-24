/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.compatibility.transport.http.HttpConstants.HEADER_AUTHORIZATION;
import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;

import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.endpoint.MuleEndpointURI;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.util.StringUtils;
import org.mule.tck.junit4.AbstractMuleContextEndpointTestCase;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HttpConnectorClientAuthorizationTestCase extends AbstractMuleContextEndpointTestCase {

  private static final String CREDENTIALS_USER = "myUser";

  private static final String CREDENTIALS_PASSWORD = "myPassword";

  private static final String URI_WITHOUT_CREDENTIALS = "http://localhost:60127";

  private static final String URI_WITH_CREDENTIALS =
      "http://" + CREDENTIALS_USER + ":" + CREDENTIALS_PASSWORD + "@localhost:60127";

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

  private Charset encoding;

  private HttpConnector connector;

  @Before
  public void setup() throws URISyntaxException {
    uri = new URI(URI_WITHOUT_CREDENTIALS);
    message = MuleMessage.builder().payload(StringUtils.EMPTY).build();
    encoding = getDefaultEncoding(muleContext);
    connector = new HttpConnector(muleContext);
    connector.setName("test");
  }

  @Test
  public void testWithUserInfo() throws Exception {
    URI uri = new URI(URI_WITH_CREDENTIALS);

    when(mockMuleEvent.getMessage()).thenReturn(message);
    when(mockImmutableEndpoint.getProperty(HEADER_AUTHORIZATION)).thenReturn(null);
    when(mockImmutableEndpoint.getEncoding()).thenReturn(encoding);
    when(mockImmutableEndpoint.getEndpointURI()).thenReturn(new MuleEndpointURI(URI_WITH_CREDENTIALS, muleContext));

    connector.setupClientAuthorization(mockMuleEvent, mockHttpMethod, mockHttpClient, mockImmutableEndpoint);

    verify(mockHttpMethod, atLeast(1)).addRequestHeader(eq(HEADER_AUTHORIZATION), anyString());
  }

  @Test
  public void testWithAuthorizationHeader() throws Exception {
    message = MuleMessage.builder(message).addOutboundProperty(HEADER_AUTHORIZATION, HEADER_AUTHORIZATION_VALUE).build();

    when(mockMuleEvent.getMessage()).thenReturn(message);
    when(mockImmutableEndpoint.getProperty(HEADER_AUTHORIZATION)).thenReturn(HEADER_AUTHORIZATION_VALUE);
    when(mockHttpMethod.getRequestHeader(HEADER_AUTHORIZATION)).thenReturn(null);

    connector.setupClientAuthorization(mockMuleEvent, mockHttpMethod, mockHttpClient, mockImmutableEndpoint);

    verify(mockHttpMethod, atLeast(1)).addRequestHeader(eq(HEADER_AUTHORIZATION), anyString());
  }

  @Test
  public void testWithProxyAuth() throws Exception {
    when(mockMuleEvent.getMessage()).thenReturn(message);

    connector.setProxyUsername(CREDENTIALS_USER);
    connector.setProxyPassword(CREDENTIALS_PASSWORD);
    connector.setupClientAuthorization(mockMuleEvent, mockHttpMethod, mockHttpClient, mockImmutableEndpoint);

    verify(mockHttpClient.getParams(), never()).setAuthenticationPreemptive(false);
  }

  @Test
  public void testClean() throws Exception {
    when(mockMuleEvent.getMessage()).thenReturn(message);

    connector.setProxyUsername(null);
    connector.setProxyPassword(null);
    connector.setupClientAuthorization(mockMuleEvent, mockHttpMethod, mockHttpClient, mockImmutableEndpoint);

    verify(mockHttpClient.getParams(), atLeast(1)).setAuthenticationPreemptive(false);
  }

}
