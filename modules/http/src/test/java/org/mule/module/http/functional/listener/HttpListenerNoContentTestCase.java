/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.HttpStatus.NO_CONTENT;
import static org.mule.module.http.api.HttpHeaders.Names.TRANSFER_ENCODING;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerNoContentTestCase extends FunctionalTestCase
{

  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Override
  protected String getConfigFile()
  {
    return "http-listener-no-content-config.xml";
  }

  @Test
  public void noBodyWhenEmpty() throws IOException
  {
    verifyResponseFrom("empty");
  }

  @Test
  public void noBodyWhenString() throws IOException
  {
    verifyResponseFrom("content");
  }

  @Test
  public void noBodyWhenEmptyUsingProperty() throws IOException
  {
    verifyResponseFrom("emptyProperty");
  }

  @Test
  public void noBodyWhenStringUsingProperty() throws IOException
  {
    verifyResponseFrom("contentProperty");
  }

  private void verifyResponseFrom(String path) throws IOException
  {
    final Response response = Request.Get(getUrl(path)).execute();
    HttpResponse httpResponse = response.returnResponse();
    assertThat(httpResponse.getEntity(), is(nullValue()));
    assertThat(httpResponse.getStatusLine().getStatusCode(), is(NO_CONTENT.getStatusCode()));
    assertThat(httpResponse.getFirstHeader(TRANSFER_ENCODING), is(nullValue()));
  }

  private String getUrl(String path)
  {
    return String.format("http://localhost:%s/%s", port.getNumber(), path);
  }

  private static class StreamingMessageProcessor implements MessageProcessor
  {
    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
      event.getMessage().setPayload(new ByteArrayInputStream(new byte[] {}));
      return event;
    }
  }

}
