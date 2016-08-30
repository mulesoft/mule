/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.request;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.http.api.HttpConstants.ResponseProperties.HTTP_REASON_PROPERTY;
import static org.mule.runtime.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.expression.DefaultExpressionManager;
import org.mule.runtime.core.util.AttributeEvaluator;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.runtime.module.http.internal.domain.response.HttpResponse;
import org.mule.runtime.module.http.internal.domain.response.HttpResponseBuilder;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;


public class HttpResponseToMuleEventTestCase extends AbstractMuleContextTestCase {

  private static final String TEST_HEADER = "TestHeader";
  private static final String TEST_MULTIPLE_HEADER = "TestMultipleHeader";
  private static final String TEST_VALUE = "TestValue";
  private DefaultHttpRequester httpRequester;
  private HttpResponseToMuleEvent httpResponseToMuleEvent;
  private HttpResponse httpResponse;
  private MuleEvent event;

  @Before
  public void setup() throws Exception {
    httpRequester = new DefaultHttpRequester();
    httpRequester.setConfig(new DefaultHttpRequesterConfig());
    final AttributeEvaluator attrEvaluator = new AttributeEvaluator("true");
    attrEvaluator.initialize(new DefaultExpressionManager());
    httpResponseToMuleEvent = new HttpResponseToMuleEvent(httpRequester, muleContext, attrEvaluator);

    HttpResponseBuilder builder = new HttpResponseBuilder();
    builder.setEntity(new InputStreamHttpEntity(new ByteArrayInputStream(TEST_MESSAGE.getBytes())));
    builder.addHeader(TEST_HEADER, TEST_VALUE);
    builder.addHeader(TEST_MULTIPLE_HEADER, TEST_VALUE);
    builder.addHeader(TEST_MULTIPLE_HEADER, TEST_VALUE);
    builder.setStatusCode(200);
    builder.setReasonPhrase("OK");
    httpResponse = builder.build();
    event = getTestEvent(MuleMessage.builder().nullPayload().build());
  }

  @Test
  public void responseHeadersAreMappedAsInboundProperties() throws MessagingException {
    httpResponseToMuleEvent.convert(event, httpResponse, null);
    assertThat(event.getMessage().getInboundProperty(TEST_HEADER), equalTo(TEST_VALUE));
    assertThat(event.getMessage().getInboundProperty(TEST_MULTIPLE_HEADER), equalTo(asList(TEST_VALUE, TEST_VALUE)));
  }

  @Test
  public void previousInboundPropertiesAreRemoved() throws Exception {
    event = getTestEvent(MuleMessage.builder().nullPayload().addInboundProperty("TestInboundProperty", TEST_VALUE).build());
    httpResponseToMuleEvent.convert(event, httpResponse, null);
    assertThat(event.getMessage().getInboundProperty("TestInboundProperty"), nullValue());
  }

  @Test
  public void responseBodyIsSetAsPayload() throws MessagingException {
    httpResponseToMuleEvent.convert(event, httpResponse, null);
    InputStream responseStream = (InputStream) event.getMessage().getPayload();
    assertThat(IOUtils.toString(responseStream), equalTo(TEST_MESSAGE));
  }

  @Test
  public void statusCodeIsSetAsInboundProperty() throws MessagingException {
    httpResponseToMuleEvent.convert(event, httpResponse, null);
    assertThat(event.getMessage().getInboundProperty(HTTP_STATUS_PROPERTY), equalTo(200));
  }

  @Test
  public void responsePhraseIsSetAsInboundProperty() throws MessagingException {
    httpResponseToMuleEvent.convert(event, httpResponse, null);
    assertThat(event.getMessage().getInboundProperty(HTTP_REASON_PROPERTY), equalTo("OK"));
  }

}
