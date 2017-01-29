/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.http;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.tck.junit4.AbstractMuleContextTestCase.RECEIVE_TIMEOUT;
import org.mule.functional.junit4.DomainFunctionalTestCase;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.util.IOUtils;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore("MULE-10633")
public class HttpSharePortTestCase extends DomainFunctionalTestCase {

  public static final String HELLO_WORLD_SERVICE_APP = "helloWorldServiceApp";
  public static final String HELLO_MULE_SERVICE_APP = "helloMuleServiceApp";

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");
  @Rule
  public SystemProperty endpointScheme = getEndpointSchemeSystemProperty();
  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder().tlsContextFactory(getTlsContextFactory()).build();

  @Override
  protected String getDomainConfig() {
    return "domain/http/http-shared-listener-config.xml";
  }

  @Override
  public ApplicationConfig[] getConfigResources() {
    return new ApplicationConfig[] {
        new ApplicationConfig(HELLO_WORLD_SERVICE_APP, new String[] {"domain/http/http-hello-world-app.xml"}),
        new ApplicationConfig(HELLO_MULE_SERVICE_APP, new String[] {"domain/http/http-hello-mule-app.xml"})};
  }

  @Test
  public void bothServicesBindCorrectly() throws Exception {
    HttpRequest httpRequest = HttpRequest.builder()
        .setUri(format("%s://localhost:%d/service/helloWorld", endpointScheme.getValue(), dynamicPort.getNumber())).build();
    HttpResponse httpResponse = httpClient.send(httpRequest, RECEIVE_TIMEOUT, false, null);

    String payload = IOUtils.toString(((InputStreamHttpEntity) httpResponse.getEntity()).getInputStream(), UTF_8);
    assertThat(payload, is("hello world"));

    httpRequest = HttpRequest.builder()
        .setUri(format("%s://localhost:%d/service/helloMule", endpointScheme.getValue(), dynamicPort.getNumber())).build();
    httpResponse = httpClient.send(httpRequest, RECEIVE_TIMEOUT, false, null);

    payload = IOUtils.toString(((InputStreamHttpEntity) httpResponse.getEntity()).getInputStream(), UTF_8);
    assertThat(payload, is("hello mule"));
  }

  protected SystemProperty getEndpointSchemeSystemProperty() {
    return new SystemProperty("scheme", "http");
  }

  protected TlsContextFactory getTlsContextFactory() {
    return null;
  }
}
