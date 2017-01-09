/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.transformers;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mule.compatibility.transport.http.HttpConstants.DEFAULT_CONTENT_TYPE;
import static org.mule.compatibility.transport.http.HttpConstants.FORM_URLENCODED_CONTENT_TYPE;
import static org.mule.compatibility.transport.http.HttpConstants.METHOD_GET;
import static org.mule.compatibility.transport.http.HttpConstants.METHOD_POST;
import static org.mule.compatibility.transport.http.HttpConstants.METHOD_PUT;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.TransformationService;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.construct.Flow;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class HttpRequestBodyToParamMapTestCase extends AbstractMuleContextTestCase {

  private MuleContext muleContext = mockContextWithServices();
  @Mock
  private TransformationService transformationService;
  private Flow flow;
  private EventContext context;

  @Before
  public void setup() throws Exception {
    when(muleContext.getTransformationService()).thenReturn(transformationService);
    when(transformationService.transform(any(InternalMessage.class), any(DataType.class)))
        .thenAnswer(inv -> (InternalMessage) inv.getArguments()[0]);

    flow = getTestFlow(muleContext);
    context = DefaultEventContext.create(flow, TEST_CONNECTOR);
  }

  @Test
  public void validGet() throws Exception {
    InternalMessage msg = createMessage(METHOD_GET, DEFAULT_CONTENT_TYPE);
    verifyTransformation(transform(Event.builder(context).message(msg).flow(flow).build()));
  }

  @Test
  public void validPost() throws Exception {
    InternalMessage msg = createMessage(METHOD_POST, FORM_URLENCODED_CONTENT_TYPE);
    verifyTransformation(transform(Event.builder(context).message(msg).flow(flow).build()));
  }

  @Test
  public void validPut() throws Exception {
    InternalMessage msg = createMessage(METHOD_PUT, FORM_URLENCODED_CONTENT_TYPE);
    verifyTransformation(transform(Event.builder(context).message(msg).flow(flow).build()));
  }

  @Test(expected = TransformerException.class)
  public void invalidContentType() throws Exception {
    InternalMessage msg = createMessage(METHOD_POST, "application/json");
    transform(Event.builder(context).message(msg).flow(flow).build());
  }

  private Object transform(Event event) throws TransformerException {
    HttpRequestBodyToParamMap transformer = new HttpRequestBodyToParamMap();
    transformer.setMuleContext(muleContext);
    return transformer.transformMessage(event, UTF_8);
  }

  private void verifyTransformation(Object payload) throws TransformerException {
    assertThat(payload instanceof Map, is(true));
    Map<String, String> map = (Map<String, String>) payload;
    assertThat(map.size(), is(2));
    assertThat(map.get("key1"), is("value1"));
    assertThat(map.get("key2"), is("value2"));
  }

  private InternalMessage createMessage(String method, String contentType) {
    Map<String, Serializable> inboundProperties = new HashMap<>();
    inboundProperties.put("http.method", method);

    String payload = "key1=value1&key2=value2";
    if ("GET".equals(method)) {
      payload = "http://localhost/?" + payload;
    }
    return InternalMessage.builder().payload(payload).inboundProperties(inboundProperties).mediaType(MediaType.parse(contentType))
        .build();
  }

}
