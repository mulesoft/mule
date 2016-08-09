/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.getConfigurationInstanceFromRegistry;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.runtime.module.http.api.HttpConstants.Protocols.HTTP;
import static org.mule.runtime.module.http.api.HttpConstants.Protocols.HTTPS;
import static org.mule.test.module.http.functional.matcher.HttpMessageAttributesMatchers.hasStatusCode;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.extension.http.internal.request.validator.HttpRequesterProvider;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.internal.connection.ConnectionProviderWrapper;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.junit.Rule;
import org.junit.Test;

public class HttpRequestFunctionalTestCase extends AbstractHttpRequestTestCase {

  private static final String TEST_HEADER_NAME = "TestHeaderName";
  private static final String TEST_HEADER_VALUE = "TestHeaderValue";
  private static final String DEFAULT_PORT_HTTP_REQUEST_CONFIG_NAME = "requestConfigHttp";
  private static final String DEFAULT_PORT_HTTPS_REQUEST_CONFIG_NAME = "requestConfigHttps";

  @Override
  protected String getConfigFile() {
    return "http-request-functional-config.xml";
  }

  @Test
  public void requestConfigDefaultPortHttp() throws Exception {
    MuleEvent testEvent = getTestEvent(TEST_PAYLOAD);
    ConfigurationInstance config = getConfigurationInstanceFromRegistry(DEFAULT_PORT_HTTP_REQUEST_CONFIG_NAME, testEvent);
    ConnectionProviderWrapper providerWrapper = (ConnectionProviderWrapper) config.getConnectionProvider().get();
    HttpRequesterProvider provider = (HttpRequesterProvider) providerWrapper.getDelegate();
    assertThat(provider.getPort().apply(testEvent), is(HTTP.getDefaultPort()));
  }

  @Test
  public void requestConfigDefaultPortHttps() throws Exception {
    MuleEvent testEvent = getTestEvent(TEST_PAYLOAD);
    ConfigurationInstance config = getConfigurationInstanceFromRegistry(DEFAULT_PORT_HTTPS_REQUEST_CONFIG_NAME, testEvent);
    ConnectionProviderWrapper providerWrapper = (ConnectionProviderWrapper) config.getConnectionProvider().get();
    HttpRequesterProvider provider = (HttpRequesterProvider) providerWrapper.getDelegate();
    assertThat(provider.getPort().apply(testEvent), is(HTTPS.getDefaultPort()));
  }

  @Test
  public void requestConfigDefaultTlsContextHttps() throws Exception {
    MuleEvent testEvent = getTestEvent(TEST_PAYLOAD);
    ConfigurationInstance config = getConfigurationInstanceFromRegistry(DEFAULT_PORT_HTTPS_REQUEST_CONFIG_NAME, testEvent);
    ConnectionProviderWrapper providerWrapper = (ConnectionProviderWrapper) config.getConnectionProvider().get();
    HttpRequesterProvider provider = (HttpRequesterProvider) providerWrapper.getDelegate();
    assertThat(provider.getTlsContext(), notNullValue());
  }

  @Test
  public void payloadIsSentAsRequestBody() throws Exception {
    flowRunner("requestFlow").withPayload(TEST_MESSAGE).run();
    assertThat(body, equalTo(TEST_MESSAGE));
  }

  @Test
  public void responseBodyIsMappedToPayload() throws Exception {
    MuleEvent event = flowRunner("requestFlow").withPayload(TEST_MESSAGE).run();
    assertTrue(event.getMessage().getPayload() instanceof InputStream);
    assertThat(getPayloadAsString(event.getMessage()), equalTo(DEFAULT_RESPONSE));
  }

  @Rule
  public DynamicPort blockingHttpPort = new DynamicPort("blockingHttpPort");

  @Test
  public void blockingResponseBodyIsMappedToPayload() throws Exception {
    MuleEvent event = flowRunner("blockingRequestFlow").withPayload(TEST_MESSAGE).run();
    assertTrue(event.getMessage().getPayload() instanceof String);
    assertThat(getPayloadAsString(event.getMessage()), equalTo("value"));
  }

  @Test
  public void responseStatusCodeIsSetAsInboundProperty() throws Exception {
    MuleEvent event = flowRunner("requestFlow").withPayload(TEST_MESSAGE).run();
    assertThat((HttpResponseAttributes) event.getMessage().getAttributes(), hasStatusCode(OK.getStatusCode()));
  }

  @Test
  public void responseHeadersAreMappedInAttributes() throws Exception {
    MuleEvent event = flowRunner("requestFlow").withPayload(TEST_MESSAGE).run();
    HttpResponseAttributes responseAttributes = (HttpResponseAttributes) event.getMessage().getAttributes();
    assertThat(responseAttributes.getHeaders(), hasEntry(TEST_HEADER_NAME.toLowerCase(), TEST_HEADER_VALUE));
  }

  @Test
  public void basePathFromConfigIsUsedInRequest() throws Exception {
    flowRunner("requestFlow").withPayload(TEST_MESSAGE).run();
    assertThat(uri, equalTo("/basePath/requestPath"));
  }

  @Test
  public void previousInboundPropertiesAreCleared() throws Exception {
    MuleEvent event =
        flowRunner("requestFlow").withPayload(TEST_MESSAGE).withInboundProperty("TestInboundProperty", "TestValue").run();
    assertThat(event.getMessage().getInboundProperty("TestInboundProperty"), nullValue());
  }

  @Override
  protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.addHeader(TEST_HEADER_NAME, TEST_HEADER_VALUE);
    super.handleRequest(baseRequest, request, response);
  }
}
