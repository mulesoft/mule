/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.mule.compatibility.core.api.transport.MuleMessageFactory;
import org.mule.compatibility.core.transport.AbstractMuleMessageFactoryTestCase;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleMessage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.httpclient.Header;
import org.junit.Test;

public class HttpMultipartMuleMessageFactoryTestCase extends AbstractMuleMessageFactoryTestCase {

  private static final String REQUEST_LINE = "POST /services/Echo HTTP/1.1";
  private static final String MULTIPART_BOUNDARY = "----------------------------299df9f9431b";
  private static final Header[] HEADERS =
      new Header[] {new Header("Content-Type", "multipart/form-data; boundary=" + MULTIPART_BOUNDARY)};
  private static final String MULTIPART_MESSAGE =
      "--" + MULTIPART_BOUNDARY + "\r\n" + "Content-Disposition: form-data; name=\"payload\"; filename=\"payload\"\r\n"
          + "Content-Type: application/octet-stream\r\n\r\n" + "part payload\r\n\r\n" + "--" + MULTIPART_BOUNDARY + "\r\n"
          + "Content-Disposition: form-data; name=\"two\"; filename=\"two\"\r\n"
          + "Content-Type: application/octet-stream\r\n\r\n" + "part two\r\n\r\n" + "--" + MULTIPART_BOUNDARY + "--\r\n\r\n";
  private static final String MULTIPART_MESSAGE_NO_PAYLOAD =
      "--" + MULTIPART_BOUNDARY + "\r\n" + "Content-Disposition: form-data; name=\"nopayload\"; filename=\"nopayload\"\r\n"
          + "Content-Type: application/octet-stream\r\n\r\n" + "part nopayload\r\n\r\n" + "--" + MULTIPART_BOUNDARY + "\r\n"
          + "Content-Disposition: form-data; name=\"dos\"; filename=\"dos\"\r\n"
          + "Content-Type: application/octet-stream\r\n\r\n" + "part dos\r\n\r\n" + "--" + MULTIPART_BOUNDARY + "--\r\n\r\n";

  @Override
  protected MuleMessageFactory doCreateMuleMessageFactory() {
    return new HttpMultipartMuleMessageFactory();
  }

  @Override
  protected Object getValidTransportMessage() throws Exception {
    RequestLine requestLine = RequestLine.parseLine(REQUEST_LINE);
    HttpRequest request = new HttpRequest(requestLine, HEADERS, null, encoding);
    return request;
  }

  @Override
  protected Object getUnsupportedTransportMessage() {
    return "this is not a valid transport message for HttpMuleMessageFactory";
  }

  @Override
  @Test
  public void testValidPayload() throws Exception {
    HttpMuleMessageFactory factory = (HttpMuleMessageFactory) createMuleMessageFactory();
    factory.setExchangePattern(MessageExchangePattern.ONE_WAY);
    HttpRequest request = createMultiPartHttpRequest(MULTIPART_MESSAGE);
    MuleMessage message = factory.create(request, encoding);
    assertNotNull(message);
    assertTrue(message.getPayload() instanceof InputStream);
  }

  @Test
  public void testValidPayloadWihtNoPayloadPart() throws Exception {
    HttpMuleMessageFactory factory = (HttpMuleMessageFactory) createMuleMessageFactory();
    factory.setExchangePattern(MessageExchangePattern.ONE_WAY);
    HttpRequest request = createMultiPartHttpRequest(MULTIPART_MESSAGE_NO_PAYLOAD);
    MuleMessage message = factory.create(request, encoding);
    assertNotNull(message);
    assertThat(message.getPayload(), is(nullValue()));
  }

  private HttpRequest createMultiPartHttpRequest(String message) throws Exception {
    RequestLine requestLine = RequestLine.parseLine(REQUEST_LINE);
    InputStream stream = new ByteArrayInputStream(message.getBytes());
    return new HttpRequest(requestLine, HEADERS, stream, encoding);
  }

}


