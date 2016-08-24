/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.transformers;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.mule.compatibility.transport.http.HttpConstants;
import org.mule.compatibility.transport.http.HttpResponse;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.TransformationService;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.junit.Test;

@SmallTest
public class MuleMessageToHttpResponseTestCase extends AbstractMuleContextTestCase {

  @Test
  public void testSetCookieOnOutbound() throws Exception {
    Cookie[] cookiesOutbound = new Cookie[2];
    cookiesOutbound[0] = new Cookie("domain", "name-out-1", "value-out-1");
    cookiesOutbound[1] = new Cookie("domain", "name-out-2", "value-out-2");

    MuleMessage msg = MuleMessage.builder(createMockMessage()).addOutboundProperty("Set-Cookie", cookiesOutbound).build();

    MuleMessageToHttpResponse transformer = getMuleMessageToHttpResponse();

    HttpResponse response = transformer.createResponse(null, UTF_8, msg, null, null, null);
    Header[] headers = response.getHeaders();
    int cookiesSet = 0;
    for (Header header : headers) {
      if ("Set-Cookie".equals(header.getName())) {
        cookiesSet++;
      }
    }
    assertThat(cookiesSet, equalTo(cookiesOutbound.length));
  }

  @Test
  public void testSetDateOnOutbound() throws Exception {
    MuleMessage msg = createMockMessage();

    MuleMessageToHttpResponse transformer = getMuleMessageToHttpResponse();
    HttpResponse response = transformer.createResponse(null, UTF_8, msg, null, null, null);
    Header[] headers = response.getHeaders();

    boolean hasDateHeader = false;
    for (Header header : headers) {
      if (HttpConstants.HEADER_DATE.equals(header.getName())) {
        hasDateHeader = true;
        // validate that the header is in the appropriate format (rfc-1123)
        SimpleDateFormat formatter = new SimpleDateFormat(HttpConstants.DATE_FORMAT_RFC822, Locale.US);
        formatter.setLenient(false);
        try {
          formatter.parse(header.getValue());
        } catch (ParseException e) {
          formatter.setLenient(true);
          formatter.parse(header.getValue());
        }
      }
    }
    assertThat("Missing 'Date' header", hasDateHeader, is(true));
  }

  private MuleMessage createMockMessage() throws TransformerException {
    MuleMessage msg = MuleMessage.builder().payload(new Object()).build();
    muleContext = spy(muleContext);
    TransformationService transformationService = mock(TransformationService.class);
    when(muleContext.getTransformationService()).thenReturn(transformationService);
    doReturn(MuleMessage.builder().payload((OutputHandler) (event, out) -> {
    }).build()).when(transformationService).transform(any(MuleMessage.class), any(DataType.class));
    return msg;
  }

  private MuleMessageToHttpResponse getMuleMessageToHttpResponse() throws Exception {
    MuleMessageToHttpResponse transformer = new MuleMessageToHttpResponse();
    transformer.setMuleContext(muleContext);
    transformer.initialise();
    return transformer;
  }
}
