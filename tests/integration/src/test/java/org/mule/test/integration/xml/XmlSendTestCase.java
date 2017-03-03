/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.xml;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mule.service.http.api.HttpConstants.HttpStatus.NOT_ACCEPTABLE;
import static org.mule.service.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.service.http.api.HttpConstants.Method.POST;

import org.mule.runtime.core.util.IOUtils;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.InputStream;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class XmlSendTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/xml/xml-conf-flow.xml";
  }

  @Test
  @Ignore("MULE-11897: When filter throws exception, the handler loses the reference to the filter")
  public void testXmlFilter() throws Exception {
    String url = "http://localhost:" + dynamicPort.getNumber() + "/xml-parse";

    InputStream xml = getClass().getResourceAsStream("request.xml");
    HttpRequest request = HttpRequest.builder().setUri(url)
        .setEntity(new InputStreamHttpEntity(xml)).setMethod(POST).build();

    HttpResponse httpResponse = httpClient.send(request, RECEIVE_TIMEOUT, false, null);
    assertThat(httpResponse.getStatusCode(), equalTo(OK.getStatusCode()));

    // This won't pass the filter
    xml = getClass().getResourceAsStream("validation1.xml");
    request = HttpRequest.builder().setUri(url).setEntity(new InputStreamHttpEntity(xml)).setMethod(POST).build();

    httpResponse = httpClient.send(request, RECEIVE_TIMEOUT, false, null);
    assertThat(httpResponse.getStatusCode(), equalTo(NOT_ACCEPTABLE.getStatusCode()));
  }

  @Test
  public void testXmlFilterAndXslt() throws Exception {
    InputStream xml = getClass().getResourceAsStream("request.xml");
    assertNotNull(xml);

    HttpRequest request = HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/xml-xslt-parse")
        .setEntity(new InputStreamHttpEntity(xml)).setMethod(POST).build();

    HttpResponse httpResponse = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    assertThat(httpResponse.getStatusCode(), equalTo(OK.getStatusCode()));
  }

  @Test
  @Ignore("MULE-11897: When filter throws exception, the handler loses the reference to the filter")
  public void testXmlValidation() throws Exception {
    String url = "http://localhost:" + dynamicPort.getNumber() + "/validate";

    InputStream xml = getClass().getResourceAsStream("validation1.xml");
    HttpRequest httpRequest = HttpRequest.builder().setUri(url)
        .setEntity(new InputStreamHttpEntity(xml)).setMethod(POST).build();
    HttpResponse httpResponse = httpClient.send(httpRequest, RECEIVE_TIMEOUT, false, null);
    assertThat(httpResponse.getStatusCode(), equalTo(OK.getStatusCode()));

    xml = getClass().getResourceAsStream("validation2.xml");
    httpRequest = HttpRequest.builder().setUri(url)
        .setEntity(new InputStreamHttpEntity(xml)).setMethod(POST).build();
    httpResponse = httpClient.send(httpRequest, RECEIVE_TIMEOUT, false, null);
    assertThat(httpResponse.getStatusCode(), equalTo(NOT_ACCEPTABLE.getStatusCode()));

    xml = getClass().getResourceAsStream("validation3.xml");
    httpRequest = HttpRequest.builder().setUri(url)
        .setEntity(new InputStreamHttpEntity(xml)).setMethod(POST).build();
    httpResponse = httpClient.send(httpRequest, RECEIVE_TIMEOUT, false, null);
    assertThat(httpResponse.getStatusCode(), equalTo(OK.getStatusCode()));
  }

  @Test
  public void testExtractor() throws Exception {
    InputStream xml = getClass().getResourceAsStream("validation1.xml");
    HttpRequest httpRequest = HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/extract")
        .setEntity(new InputStreamHttpEntity(xml)).setMethod(POST).build();
    HttpResponse httpResponse = httpClient.send(httpRequest, RECEIVE_TIMEOUT, false, null);

    String payload = IOUtils.toString(((InputStreamHttpEntity) httpResponse.getEntity()).getInputStream(), UTF_8);
    assertThat(payload, equalTo("some"));
  }
}
