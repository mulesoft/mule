/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.functional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;

import org.mule.compatibility.transport.http.HttpRequest;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.ClassRule;
import org.junit.Test;

public class HttpOutboundThrowExceptionTestCase extends AbstractMockHttpServerTestCase {

  @ClassRule
  public static DynamicPort inboundPort = new DynamicPort("portIn");

  @ClassRule
  public static DynamicPort outboundPort = new DynamicPort("portOut");

  private Latch testLatch = new Latch();

  public HttpOutboundThrowExceptionTestCase() {
    setDisposeContextPerClass(true);
  }

  @Override
  protected String getConfigFile() {
    return "http-outbound-throw-exception-config.xml";
  }

  @Override
  protected MockHttpServer getHttpServer() {
    return new SimpleHttpServer(outboundPort.getNumber());
  }

  @Test
  public void errorStatusPropagation() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage result =
        client.send("errorPropagationEndpoint",
                    MuleMessage.builder().payload(TEST_MESSAGE).mediaType(MediaType.parse("text/plain;charset=UTF-8")).build())
            .getRight();
    assertThat((String) result.getInboundProperty("http.status"), is("400"));
  }

  @Test
  public void errorStatusThrowException() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage result =
        client.send("exceptionOnErrorStatusEndpoint",
                    MuleMessage.builder().payload(TEST_MESSAGE).mediaType(MediaType.parse("text/plain;charset=UTF-8")).build())
            .getRight();
    assertThat((String) result.getInboundProperty("http.status"), is("500"));
  }

  private class SimpleHttpServer extends SingleRequestMockHttpServer {

    private static final String HTTP_STATUS_LINE_BAD_REQUEST = "HTTP/1.1 400 Bad Request\n";

    public SimpleHttpServer(int listenPort) {
      super(listenPort, getDefaultEncoding(muleContext), HTTP_STATUS_LINE_BAD_REQUEST);
    }

    @Override
    protected void processSingleRequest(HttpRequest httpRequest) throws Exception {
      testLatch.countDown();
    }
  }
}
