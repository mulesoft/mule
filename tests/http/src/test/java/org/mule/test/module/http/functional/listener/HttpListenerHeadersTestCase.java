/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.listener;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.OK;
import org.mule.runtime.core.util.IOUtils;
import org.mule.test.module.http.functional.AbstractHttpTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.message.BasicHeader;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerHeadersTestCase extends AbstractHttpTestCase {

  @Rule
  public DynamicPort listenPort = new DynamicPort("port");
  @Rule
  public SystemProperty header = new SystemProperty("header", "custom");

  @Override
  protected String getConfigFile() {
    return "http-listener-headers-config.xml";
  }

  @Test
  public void handlesEmptyHeader() throws Exception {
    testHeaders("emptyHeader", EMPTY, new BasicHeader(header.getValue(), null));
  }

  @Test
  public void handlesSimpleHeader() throws Exception {
    testHeaders("simpleHeader", "custom1", new BasicHeader(header.getValue(), "custom1"));
  }

  @Test
  public void handlesMultipleHeadersString() throws Exception {
    testHeaders("multipleHeadersString", "custom2", new BasicHeader(header.getValue(), "custom1"),
                new BasicHeader(header.getValue(), "custom2"));
  }

  @Test
  public void handlesMultipleHeadersCollection() throws Exception {
    testHeaders("multipleHeadersCollection", "custom1", new BasicHeader(header.getValue(), "custom1"),
                new BasicHeader(header.getValue(), "custom2"));
  }

  public void testHeaders(String path, String expectedResponse, Header... headers) throws IOException {
    String url = String.format("http://localhost:%s/%s", listenPort.getNumber(), path);
    HttpResponse response = Request.Post(url).setHeaders(headers).execute().returnResponse();

    assertThat(response.getStatusLine().getStatusCode(), is(OK.getStatusCode()));
    assertThat(IOUtils.toString(response.getEntity().getContent()), is(expectedResponse));
  }


}
