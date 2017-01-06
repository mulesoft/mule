/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.components;

import static org.mockito.Mockito.mock;

import org.mule.compatibility.transport.http.AbstractDateHeaderTestCase;
import org.mule.compatibility.transport.http.HttpResponse;
import org.mule.compatibility.transport.http.components.HttpResponseBuilder;
import org.mule.runtime.core.api.MuleContext;

import java.util.Date;

import org.junit.Before;

public class HttpResponseBuilderDateHeaderTestCase extends AbstractDateHeaderTestCase {

  private static final String EXPECTED_DATE_HEADER = "Mon, 05 Sep 2005 21:30:00 +0000";
  private MuleContext muleContext;
  private HttpResponseBuilder httpResponseBuilder;

  @Before
  public void setUp() {
    muleContext = mock(MuleContext.class);
  }

  @Override
  protected void initialise() throws Exception {
    HttpResponseBuilder httpResponseBuilder = new HttpResponseBuilder();
    httpResponseBuilder.setMuleContext(muleContext);
    httpResponseBuilder.initialise();
    this.httpResponseBuilder = httpResponseBuilder;
  }

  @Override
  protected void setDateHeader(HttpResponse response, long millis) {
    httpResponseBuilder.setDateHeader(response, new Date(millis));
  }

  @Override
  protected String getExpectedHeaderValue() {
    return EXPECTED_DATE_HEADER;
  }

}
