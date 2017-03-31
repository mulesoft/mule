/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.service.http.api.HttpConstants.Method.POST;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.retry.RetryPolicyExhaustedException;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.tls.internal.DefaultTlsContextFactory;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.HttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Error Handling")
@Stories("On Error Continue")
public class OnErrorContinueTestCase extends AbstractIntegrationTestCase {

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

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).tlsContextFactory(() -> {
    try {
      DefaultTlsContextFactory tlsContextFactory = new DefaultTlsContextFactory();
      // Configure trust store in the client with the certificate of the server.
      tlsContextFactory.setTrustStorePath("ssltest-cacerts.jks");
      tlsContextFactory.setTrustStorePassword("changeit");

      return tlsContextFactory;
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
  }).build();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/on-error-continue-use-case-flow.xml";
  }

  @Test
  public void testHttpJsonErrorResponse() throws Exception {
    testJsonErrorResponse(String.format("http://localhost:%s/service", dynamicPort1.getNumber()));
  }

  @Test
  public void testHttpsJsonErrorResponse() throws Exception {
    testJsonErrorResponse(String.format("https://localhost:%s/httpsservice", dynamicPort3.getNumber()));
  }

  @Test
  public void testJsonErrorResponse() throws Exception {
    assertResponse(flowRunner("continueProcessingActualMessage").withPayload(JSON_REQUEST).run().getMessage());
  }

  private void assertResponse(Message response) throws Exception {
    assertThat(response, IsNull.<Object>notNullValue());
    // compare the structure and values but not the attributes' order
    JsonNode actualJsonNode = new ObjectMapper().readTree(getPayloadAsString(response));
    JsonNode expectedJsonNode = new ObjectMapper().readTree(JSON_RESPONSE);
    assertThat(actualJsonNode, Is.is(expectedJsonNode));
  }

  private void testJsonErrorResponse(String endpointUri) throws Exception {
    HttpRequest request = HttpRequest.builder().setUri(endpointUri).setMethod(POST)
        .setEntity(new ByteArrayHttpEntity(JSON_REQUEST.getBytes())).build();
    final HttpEntity response = httpClient.send(request, TIMEOUT, false, null).getEntity();

    assertResponse(response);
  }

  private void assertResponse(HttpEntity responseEntity) throws Exception {
    assertThat(responseEntity, IsNull.<Object>notNullValue());
    // compare the structure and values but not the attributes' order
    JsonNode actualJsonNode =
        new ObjectMapper().readTree(IOUtils.toString(((InputStreamHttpEntity) responseEntity).getInputStream()));
    JsonNode expectedJsonNode = new ObjectMapper().readTree(JSON_RESPONSE);
    assertThat(actualJsonNode, Is.is(expectedJsonNode));
  }

  public static final String MESSAGE = "some message";
  public static final String MESSAGE_EXPECTED = "some message consumed successfully";

  @Test
  public void testCatchWithComponent() throws Exception {
    Message result = flowRunner("catchWithComponent").withPayload(MESSAGE).run().getMessage();
    assertThat(result, IsNull.<Object>notNullValue());
    assertThat(getPayloadAsString(result), Is.is(MESSAGE + " Caught"));
  }

  @Test
  public void testFullyDefinedCatchExceptionStrategyWithComponent() throws Exception {
    Message result =
        flowRunner("fullyDefinedCatchExceptionStrategyWithComponent").withPayload(MESSAGE).run().getMessage();
    assertThat(result, IsNull.<Object>notNullValue());
    assertThat(getPayloadAsString(result), Is.is(MESSAGE + " apt1 apt2 groovified"));
  }

  @Test
  public void onErrorTypeMatch() throws Exception {
    Message result = flowRunner("onErrorTypeMatch").withPayload(MESSAGE).run().getMessage();
    assertThat(result, is(notNullValue()));
    assertThat(getPayloadAsString(result), is(MESSAGE + " apt1 apt2"));
  }

  @Test
  public void onErrorTypeMatchAny() throws Exception {
    Message result = flowRunner("onErrorTypeMatchAny").withPayload(MESSAGE).run().getMessage();
    assertThat(result, is(notNullValue()));
    assertThat(getPayloadAsString(result), is(MESSAGE + " apt1 apt2"));
  }

  @Test
  public void onErrorTypeMatchSeveral() throws Exception {
    Message result = flowRunner("onErrorTypeMatchSeveral").withPayload(true).run().getMessage();
    assertThat(result, is(notNullValue()));
    assertThat(getPayloadAsString(result), is("true apt1 apt2"));

    result = flowRunner("onErrorTypeMatchSeveral").withPayload(false).run().getMessage();
    assertThat(result, is(notNullValue()));
    assertThat(getPayloadAsString(result), is("false apt1 apt2"));
  }

  public static class FailingProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      throw new RetryPolicyExhaustedException(createStaticMessage("Error."), new Object());
    }

  }

  public static class LoadNewsProcessor extends AbstractAnnotatedObject implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      NewsRequest newsRequest;

      try {
        Object payload = event.getMessage().getPayload().getValue();
        if (payload instanceof CursorStreamProvider) {
          newsRequest = handleInputStream(((CursorStreamProvider) payload).openCursor());
        } else if (payload instanceof InputStream) {
          newsRequest = handleInputStream((InputStream) payload);
        } else if (payload instanceof String) {
          newsRequest = new ObjectMapper().readValue((String) payload, NewsRequest.class);
        } else {
          throw new RuntimeException("Cannot create an object from a " + payload.getClass().getName());
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      NewsResponse newsResponse = new NewsResponse();
      newsResponse.setUserId(newsRequest.getUserId());
      newsResponse.setTitle("News title");
      return Event.builder(event).message(InternalMessage.builder(event.getMessage()).payload(newsResponse).build()).build();
    }

    private NewsRequest handleInputStream(InputStream payload) throws IOException {
      NewsRequest newsRequest;
      InputStreamReader inputStreamReader =
          new InputStreamReader(payload, "UTF-8");

      newsRequest = new ObjectMapper().readValue(inputStreamReader, NewsRequest.class);
      return newsRequest;
    }
  }

  public static class NewsErrorProcessor extends AbstractAnnotatedObject implements Processor {

    @Override
    public Event process(Event event) throws MuleException {

      NewsResponse newsResponse = (NewsResponse) event.getMessage().getPayload().getValue();
      newsResponse.setErrorMessage(ERROR_PROCESSING_NEWS);

      StringWriter writer = new StringWriter();
      try {
        new ObjectMapper().writeValue(writer, newsResponse);
      } catch (IOException e) {
        throw new DefaultMuleException(e);
      }

      return Event.builder(event).message(InternalMessage.builder(event.getMessage()).payload(writer.toString()).build()).build();
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
