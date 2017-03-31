/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.listener;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.http.api.HttpHeaders.Names.TRANSFER_ENCODING;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.service.http.api.HttpConstants.HttpStatus.NO_CONTENT;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.module.http.functional.AbstractHttpTestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerNoContentTestCase extends AbstractHttpTestCase {

  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Override
  protected String getConfigFile() {
    return "http-listener-no-content-config.xml";
  }

  @Test
  public void noBodyWhenEmpty() throws IOException {
    verifyResponseFrom("empty");
  }

  @Test
  public void noBodyWhenString() throws IOException {
    verifyResponseFrom("content");
  }

  private void verifyResponseFrom(String path) throws IOException {
    final Response response = Request.Get(getUrl(path)).execute();
    HttpResponse httpResponse = response.returnResponse();
    assertThat(httpResponse.getEntity(), is(nullValue()));
    assertThat(httpResponse.getStatusLine().getStatusCode(), is(NO_CONTENT.getStatusCode()));
    assertThat(httpResponse.getFirstHeader(TRANSFER_ENCODING), is(nullValue()));
  }

  private String getUrl(String path) {
    return String.format("http://localhost:%s/%s", port.getNumber(), path);
  }

  private static class StreamingProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      return Event.builder(event).message(of(new ByteArrayInputStream(new byte[] {}))).build();
    }
  }

}
