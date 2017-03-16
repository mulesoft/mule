/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.extension.http.internal.HttpStreamingType.AUTO;
import static org.mule.runtime.api.metadata.DataType.INPUT_STREAM;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_LENGTH;
import org.mule.extension.http.api.listener.builder.HttpListenerResponseBuilder;
import org.mule.extension.http.internal.listener.HttpResponseFactory;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.TransformationService;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("HTTP Connector")
@Stories("Issues")
public class HttpResponseFactoryTestCase extends AbstractMuleTestCase {

  private static final String EXAMPLE_STRING = "exampleString";
  private static final String WRONG_CONTENT_LENGTH = "12";

  @Test
  @Description("Verifies that the correct Content-Length is sent even when a wrong one is set as header.")
  public void testContentLengthIsOverridden() throws Exception {
    HttpListenerResponseBuilder listenerResponseBuilder = mock(HttpListenerResponseBuilder.class);
    TypedValue<Object> payload = new TypedValue<>(new ByteArrayInputStream(EXAMPLE_STRING.getBytes(UTF_8)), INPUT_STREAM);
    when(listenerResponseBuilder.getBody()).thenReturn(payload);
    Map<String, String> headers = new HashMap<>();
    headers.put(CONTENT_LENGTH, WRONG_CONTENT_LENGTH);
    when(listenerResponseBuilder.getHeaders()).thenReturn(headers);
    HttpResponseFactory httpResponseBuilder = new HttpResponseFactory(AUTO, mock(TransformationService.class));

    HttpResponse httpResponse = httpResponseBuilder.create(HttpResponse.builder(), listenerResponseBuilder, true);
    assertThat(httpResponse.getHeaderValue(CONTENT_LENGTH), is(String.valueOf(EXAMPLE_STRING.length())));
  }

}
