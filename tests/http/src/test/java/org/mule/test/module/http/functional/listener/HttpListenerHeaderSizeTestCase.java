/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.listener;

import static java.lang.String.format;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.BAD_REQUEST;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.runtime.module.http.internal.HttpParser.appendQueryParam;
import static org.mule.runtime.module.http.internal.listener.grizzly.GrizzlyServerManager.MAXIMUM_HEADER_SECTION_SIZE_PROPERTY_KEY;
import org.mule.test.module.http.functional.AbstractHttpTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.fluent.Request;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerHeaderSizeTestCase extends AbstractHttpTestCase {

  private static final int SIZE_DELTA = 1000;

  @Rule
  public SystemProperty maxHeaderSectionSizeSystemProperty =
      new SystemProperty(MAXIMUM_HEADER_SECTION_SIZE_PROPERTY_KEY, "10000");
  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port");

  @Override
  protected String getConfigFile() {
    return "http-listener-max-header-size-config.xml";
  }

  @Test
  public void maxHeaderSizeExceeded() throws Exception {
    HttpResponse response =
        sendRequestWithQueryParam(Integer.valueOf(maxHeaderSectionSizeSystemProperty.getValue()) + SIZE_DELTA);
    StatusLine statusLine = response.getStatusLine();
    assertThat(statusLine.getStatusCode(), is(BAD_REQUEST.getStatusCode()));
    assertThat(statusLine.getReasonPhrase(), is(BAD_REQUEST.getReasonPhrase()));
  }

  @Test
  public void maxHeaderSizeNotExceeded() throws Exception {
    int queryParamSize = Integer.valueOf(maxHeaderSectionSizeSystemProperty.getValue()) - SIZE_DELTA;
    HttpResponse response = sendRequestWithQueryParam(queryParamSize);
    assertThat(response.getStatusLine().getStatusCode(), is(OK.getStatusCode()));
    assertThat((int) response.getEntity().getContentLength(), is(queryParamSize));
  }

  private HttpResponse sendRequestWithQueryParam(int queryParamSize) throws Exception {
    String longQueryParamValue = RandomStringUtils.randomAlphanumeric(queryParamSize);
    String urlWithQueryParameter =
        appendQueryParam(format("http://localhost:%d/", dynamicPort.getNumber()), "longQueryParam", longQueryParamValue);
    return Request.Post(urlWithQueryParameter).execute().returnResponse();
  }
}
