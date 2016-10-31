/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.http.api.HttpConstants.Methods.POST;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.module.http.api.client.HttpRequestOptions;
import org.mule.runtime.module.tls.internal.DefaultTlsContextFactory;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OnErrorContinueTransportTestCase extends CompatibilityFunctionalTestCase {

  public static final int TIMEOUT = 5000;
  public static final String ERROR_PROCESSING_NEWS = "error processing news";
  public static final String JSON_RESPONSE =
      "{\"errorMessage\":\"error processing news\",\"userId\":15,\"title\":\"News title\"}";
  public static final String JSON_REQUEST = "{\"userId\":\"15\"}";

  @Rule
  public DynamicPort dynamicPort1 = new DynamicPort("port1");
  @Rule
  public DynamicPort dynamicPort2 = new DynamicPort("port2");
  @Rule
  public DynamicPort dynamicPort3 = new DynamicPort("port3");

  private DefaultTlsContextFactory tlsContextFactory;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/on-error-continue-use-case-flow.xml";
  }

  @Before
  public void setup() throws IOException {
    tlsContextFactory = new DefaultTlsContextFactory();

    // Configure trust store in the client with the certificate of the server.
    tlsContextFactory.setTrustStorePath("trustStore");
    tlsContextFactory.setTrustStorePassword("mulepassword");
  }

  @Test
  public void testTcpJsonErrorResponse() throws Exception {
    testJsonErrorResponse(String.format("tcp://localhost:%s", dynamicPort2.getNumber()));
  }

  private void testJsonErrorResponse(String endpointUri) throws Exception {
    MuleClient client = muleContext.getClient();
    final HttpRequestOptions httpRequestOptions =
        newOptions().method(POST.name()).tlsContextFactory(tlsContextFactory).responseTimeout(TIMEOUT).build();
    InternalMessage response = client.send(endpointUri, InternalMessage.of(JSON_REQUEST), httpRequestOptions).getRight();
    assertThat(response, IsNull.<Object>notNullValue());
    // compare the structure and values but not the attributes' order
    ObjectMapper mapper = new ObjectMapper();
    JsonNode actualJsonNode = mapper.readTree(getPayloadAsString(response));
    JsonNode expectedJsonNode = mapper.readTree(JSON_RESPONSE);
    assertThat(actualJsonNode, is(expectedJsonNode));
  }

  public static final String MESSAGE = "some message";
  public static final String MESSAGE_EXPECTED = "some message consumed successfully";

  public static class LoadNewsProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      NewsRequest newsRequest = (NewsRequest) event.getMessage().getPayload().getValue();
      NewsResponse newsResponse = new NewsResponse();
      newsResponse.setUserId(newsRequest.getUserId());
      newsResponse.setTitle("News title");
      return Event.builder(event).message(InternalMessage.builder(event.getMessage()).payload(newsResponse).build()).build();
    }
  }

  public static class NewsErrorProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      ((NewsResponse) event.getMessage().getPayload().getValue()).setErrorMessage(ERROR_PROCESSING_NEWS);
      return event;
    }
  }

  public static class NewsRequest {

    private int userId;

    public int getUserId() {
      return userId;
    }

    public void setUserId(int userId) {
      this.userId = userId;
    }
  }

  public static class NewsResponse {

    private int userId;
    private String title;
    private String errorMessage;

    public int getUserId() {
      return userId;
    }

    public void setUserId(int userId) {
      this.userId = userId;
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getErrorMessage() {
      return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
    }
  }

  @WebService
  public static class Echo {

    @WebResult(name = "text")
    public String echo(@WebParam(name = "text") String string) {
      throw new RuntimeException();
    }
  }

}
