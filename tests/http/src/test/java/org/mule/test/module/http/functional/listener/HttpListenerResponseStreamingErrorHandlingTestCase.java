/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.listener;

import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_EXTENSION;
import static org.mule.test.allure.AllureConstants.HttpFeature.HttpStory.ERROR_HANDLING;
import static org.apache.http.client.fluent.Request.Get;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Response;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(HTTP_EXTENSION)
@Stories({ERROR_HANDLING, "Streaming"})
public class HttpListenerResponseStreamingErrorHandlingTestCase extends AbstractHttpListenerErrorHandlingTestCase {

  @Override
  protected String getConfigFile() {
    return "http-listener-response-streaming-exception-strategy-config.xml";
  }

  @Test
  public void exceptionHandledWhenBuildingResponse() throws Exception {
    final Response response =
        Get(getUrl("exceptionBuildingResponse")).connectTimeout(DEFAULT_TIMEOUT).socketTimeout(DEFAULT_TIMEOUT).execute();

    final HttpResponse httpResponse = response.returnResponse();

    assertExceptionStrategyExecuted(httpResponse);
  }

  @Test
  public void exceptionNotHandledWhenSendingResponse() throws Exception {
    final Response response =
        Get(getUrl("exceptionSendingResponse")).connectTimeout(DEFAULT_TIMEOUT).socketTimeout(DEFAULT_TIMEOUT).execute();

    final HttpResponse httpResponse = response.returnResponse();

    assertExceptionStrategyNotExecuted(httpResponse);
  }

  @Test
  public void exceptionHandledWhenBuildingResponseFailAgain() throws Exception {
    final Response response = Get(getUrl("exceptionBuildingResponseFailAgain")).connectTimeout(DEFAULT_TIMEOUT)
        .socketTimeout(DEFAULT_TIMEOUT).execute();

    final HttpResponse httpResponse = response.returnResponse();

    assertExceptionStrategyFailed(httpResponse);
  }

  @Test
  public void exceptionNotHandledWhenSendingResponseFailAgain() throws Exception {
    final Response response =
        Get(getUrl("exceptionSendingResponseFailAgain")).connectTimeout(DEFAULT_TIMEOUT).socketTimeout(DEFAULT_TIMEOUT).execute();

    final HttpResponse httpResponse = response.returnResponse();

    assertExceptionStrategyNotExecuted(httpResponse);
  }


}
