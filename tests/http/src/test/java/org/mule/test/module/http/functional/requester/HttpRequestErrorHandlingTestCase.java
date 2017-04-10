/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.requester;

import static org.mule.extension.http.api.error.HttpError.RESPONSE_VALIDATION;
import static org.mule.extension.http.internal.listener.HttpListener.HTTP_NAMESPACE;
import static org.mule.service.http.api.HttpConstants.HttpStatus.BAD_REQUEST;
import static org.mule.service.http.api.HttpConstants.HttpStatus.EXPECTATION_FAILED;
import static org.mule.service.http.api.HttpConstants.HttpStatus.FORBIDDEN;
import static org.mule.service.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.mule.service.http.api.HttpConstants.HttpStatus.METHOD_NOT_ALLOWED;
import static org.mule.service.http.api.HttpConstants.HttpStatus.NOT_ACCEPTABLE;
import static org.mule.service.http.api.HttpConstants.HttpStatus.NOT_FOUND;
import static org.mule.service.http.api.HttpConstants.HttpStatus.SERVICE_UNAVAILABLE;
import static org.mule.service.http.api.HttpConstants.HttpStatus.TOO_MANY_REQUESTS;
import static org.mule.service.http.api.HttpConstants.HttpStatus.UNAUTHORIZED;
import static org.mule.service.http.api.HttpConstants.HttpStatus.UNSUPPORTED_MEDIA_TYPE;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_EXTENSION;
import static org.mule.test.allure.AllureConstants.HttpFeature.HttpStory.ERROR_HANDLING;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.functional.junit4.FlowRunner;
import org.mule.functional.junit4.rules.ExpectedError;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.service.http.api.HttpConstants.HttpStatus;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(HTTP_EXTENSION)
@Stories({ERROR_HANDLING, "Errors"})
public class HttpRequestErrorHandlingTestCase extends AbstractHttpRequestTestCase {

  @Rule
  public DynamicPort unusedPort = new DynamicPort("unusedPort");
  @Rule
  public ExpectedError expectedError = ExpectedError.none();

  private int serverStatus = 200;
  private String serverContentType = "text/html";
  private boolean timeout = false;
  private Latch done = new Latch();

  @Override
  protected String getConfigFile() {
    return "http-request-errors-config.xml";
  }

  @Test
  public void badRequest() throws Exception {
    verifyErrorWhenReceiving(BAD_REQUEST);
  }

  @Test
  public void unauthorised() throws Exception {
    verifyErrorWhenReceiving(UNAUTHORIZED);
  }

  @Test
  public void forbidden() throws Exception {
    verifyErrorWhenReceiving(FORBIDDEN);
  }

  @Test
  public void notFound() throws Exception {
    verifyErrorWhenReceiving(NOT_FOUND);
  }

  @Test
  public void methodNotAllowed() throws Exception {
    verifyErrorWhenReceiving(METHOD_NOT_ALLOWED);
  }

  @Test
  public void notAcceptable() throws Exception {
    verifyErrorWhenReceiving(NOT_ACCEPTABLE);
  }

  @Test
  public void unsupportedMediaType() throws Exception {
    verifyErrorWhenReceiving(UNSUPPORTED_MEDIA_TYPE);
  }

  @Test
  public void tooManyRequest() throws Exception {
    verifyErrorWhenReceiving(TOO_MANY_REQUESTS);
  }

  @Test
  public void serverError() throws Exception {
    verifyErrorWhenReceiving(INTERNAL_SERVER_ERROR);
  }

  @Test
  public void serverUnavailable() throws Exception {
    verifyErrorWhenReceiving(SERVICE_UNAVAILABLE);
  }

  @Test
  public void notMappedStatus() throws Exception {
    verifyErrorWhenReceiving(EXPECTATION_FAILED, "417 not understood", RESPONSE_VALIDATION.name());
  }

  @Test
  public void timeout() throws Exception {
    timeout = true;
    Event result = getFlowRunner("handled", httpPort.getNumber()).run();
    done.release();
    assertThat(result.getMessage().getPayload().getValue(), is("Timeout exceeded timeout"));
  }

  @Test
  public void connectivity() throws Exception {
    Event result = getFlowRunner("handled", unusedPort.getNumber()).run();
    assertThat(result.getMessage().getPayload().getValue(), is("Connection refused connectivity"));
  }

  @Test
  public void parsing() throws Exception {
    serverContentType = "multipart/form-data; boundary=\"sdgksdg\"";
    InputStream mockStream = mock(InputStream.class);
    when(mockStream.read()).thenThrow(IOException.class);
    Event result = getFlowRunner("handled", httpPort.getNumber()).run();
    assertThat(result.getMessage().getPayload().getValue(), is("Unable to process multipart response parsing"));
  }

  void verifyErrorWhenReceiving(HttpStatus status) throws Exception {
    verifyErrorWhenReceiving(status, String.format("%s %s", status.getStatusCode(), status.getReasonPhrase()), status.name());
  }

  void verifyErrorWhenReceiving(HttpStatus status, Object expectedResult, String expectedError) throws Exception {
    serverStatus = status.getStatusCode();
    // Hit flow with error handler
    Event result = getFlowRunner("handled", httpPort.getNumber()).run();
    assertThat(result.getMessage().getPayload().getValue(), is(expectedResult));
    // Hit flow that will throw back the error
    this.expectedError.expectErrorType(HTTP_NAMESPACE.toUpperCase(), expectedError);
    getFlowRunner("unhandled", httpPort.getNumber()).run();
  }

  private FlowRunner getFlowRunner(String flowName, int port) {
    return flowRunner(flowName).withVariable("port", port);
  }

  @Override
  protected void writeResponse(HttpServletResponse response) throws IOException {
    if (timeout) {
      try {
        done.await();
      } catch (InterruptedException e) {
        // Do nothing
      }
    }
    response.setContentType(serverContentType);
    response.setStatus(serverStatus);
    response.getWriter().print(DEFAULT_RESPONSE);
  }

}
