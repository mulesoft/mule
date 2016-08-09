/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.listener;

import static org.apache.http.client.fluent.Request.Get;
import static org.apache.http.client.fluent.Request.Post;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_LENGTH;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.TRANSFER_ENCODING;
import static org.mule.runtime.module.http.api.HttpHeaders.Values.CHUNKED;
import org.mule.runtime.core.util.IOUtils;
import org.mule.test.module.http.functional.AbstractHttpTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.IOException;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.fluent.Response;
import org.junit.Rule;

public abstract class HttpListenerResponseStreamingTestCase extends AbstractHttpTestCase {

  private static final int DEFAULT_TIMEOUT = 1000;

  public static final String TEST_BODY = RandomStringUtils.randomAlphabetic(100 * 1024);
  @Rule
  public SystemProperty stringPayloadLength = new SystemProperty("stringPayloadLength", String.valueOf(TEST_BODY.length()));
  @Rule
  public DynamicPort listenPort = new DynamicPort("port");

  protected abstract HttpVersion getHttpVersion();

  @Override
  protected String getConfigFile() {
    return "http-listener-response-streaming-config.xml";
  }

  protected void testResponseIsContentLengthEncoding(String url, HttpVersion httpVersion) throws IOException {
    final Response response =
        Get(url).version(httpVersion).connectTimeout(DEFAULT_TIMEOUT).socketTimeout(DEFAULT_TIMEOUT).execute();
    final HttpResponse httpResponse = response.returnResponse();
    final Header transferEncodingHeader = httpResponse.getFirstHeader(TRANSFER_ENCODING);
    final Header contentLengthHeader = httpResponse.getFirstHeader(CONTENT_LENGTH);
    assertThat(contentLengthHeader, notNullValue());
    assertThat(transferEncodingHeader, nullValue());
    assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is(TEST_BODY));
  }

  protected String getUrl(String path) {
    return String.format("http://localhost:%s/%s", listenPort.getNumber(), path);
  }

  protected void testResponseIsChunkedEncoding(String url, HttpVersion httpVersion) throws IOException {
    final Response response = Post(url).version(httpVersion).connectTimeout(DEFAULT_TIMEOUT).socketTimeout(DEFAULT_TIMEOUT)
        .bodyByteArray(TEST_BODY.getBytes()).execute();
    final HttpResponse httpResponse = response.returnResponse();
    final Header transferEncodingHeader = httpResponse.getFirstHeader(TRANSFER_ENCODING);
    final Header contentLengthHeader = httpResponse.getFirstHeader(CONTENT_LENGTH);
    assertThat(contentLengthHeader, nullValue());
    assertThat(transferEncodingHeader, notNullValue());
    assertThat(transferEncodingHeader.getValue(), is(CHUNKED));
    assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is(TEST_BODY));
  }

  protected void testResponseIsNotChunkedEncoding(String url, HttpVersion httpVersion) throws IOException {
    final Response response = Post(url).version(httpVersion).connectTimeout(DEFAULT_TIMEOUT).socketTimeout(DEFAULT_TIMEOUT)
        .bodyByteArray(TEST_BODY.getBytes()).execute();
    final HttpResponse httpResponse = response.returnResponse();
    final Header transferEncodingHeader = httpResponse.getFirstHeader(TRANSFER_ENCODING);
    final Header contentLengthHeader = httpResponse.getFirstHeader(CONTENT_LENGTH);
    assertThat(contentLengthHeader, nullValue());
    assertThat(transferEncodingHeader, is(nullValue()));
    assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is(TEST_BODY));
  }

}
