/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.module.http.functional.requester;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.junit.Rule;
import org.junit.Test;

public class HttpRequestResponseHeadersTestCase extends AbstractHttpRequestTestCase {

  private static final String EMPTY_PATH = "empty";
  private static final String SIMPLE_PATH = "simple";

  @Rule
  public SystemProperty header = new SystemProperty("header", "custom");

  @Override
  protected String getConfigFile() {
    return "http-request-response-headers-config.xml";
  }

  @Override
  protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
    String path = request.getPathInfo().substring(1);
    if (EMPTY_PATH.equals(path)) {
      response.addHeader(header.getValue(), EMPTY);
    } else if (SIMPLE_PATH.equals(path)) {
      response.addHeader(header.getValue(), "custom1");
    } else {
      // Must be "multiple"
      response.addHeader(header.getValue(), "custom1");
      response.addHeader(header.getValue(), "custom2");
    }
    super.writeResponse(response);
  }

  @Test
  public void handlesEmptyHeader() throws Exception {
    testHeaders(EMPTY_PATH, EMPTY);
  }

  @Test
  public void handlesSimpleHeader() throws Exception {
    testHeaders(SIMPLE_PATH, "custom1");
  }

  @Test
  public void handlesMultipleHeadersString() throws Exception {
    testHeaders("multipleString", "custom2");
  }

  @Test
  public void handlesMultipleHeadersCollection() throws Exception {
    testHeaders("multipleCollection", "custom1");
  }

  private void testHeaders(String flowName, String expectedResponse) throws Exception {
    MuleMessage response = flowRunner(flowName).run().getMessage();
    assertThat(response.getPayload(), is(expectedResponse));
  }
}
