/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.reliability;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mule.compatibility.transport.http.HttpConstants.SC_INTERNAL_SERVER_ERROR;

import org.mule.compatibility.transport.http.HttpConstants;

import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.PostMethod;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

/**
 * Verify that no inbound messages are lost when exceptions occur. The message must either make it all the way to the SEDA queue
 * (in the case of an asynchronous inbound endpoint), or be restored/rolled back at the source.
 * <p/>
 * In the case of the HTTP transport, there is no way to restore the source message so an exception is simply returned to the
 * client.
 */
public class InboundMessageLossAsynchTestCase extends InboundMessageLossTestCase {

  @Override
  protected String getConfigFile() {
    return "reliability/inbound-message-loss-asynch.xml";
  }

  @Test
  @Override
  public void testNoException() throws Exception {
    HttpMethodBase request = createRequest(getBaseUri() + "/noException");
    int status = httpClient.executeMethod(request);
    assertEquals(HttpConstants.SC_OK, status);
  }

  @Test
  @Override
  public void testHandledTransformerException() throws Exception {
    HttpMethodBase request = createRequest(getBaseUri() + "/handledTransformerException");
    int status = httpClient.executeMethod(request);
    assertEquals(HttpConstants.SC_OK, status);
  }

  @Test
  @Override
  public void testComponentException() throws Exception {
    HttpMethodBase request = createRequest(getBaseUri() + "/componentException");
    assertThat(httpClient.executeMethod(request), equalTo(SC_INTERNAL_SERVER_ERROR));
  }

  @Test
  @Override
  public void testTransformerException() throws Exception {
    HttpMethodBase request = createRequest(getBaseUri() + "/transformerException");
    assertThat(httpClient.executeMethod(request), equalTo(SC_INTERNAL_SERVER_ERROR));
  }

  @Test
  @Override
  public void testRouterException() throws Exception {
    HttpMethodBase request = createRequest(getBaseUri() + "/routerException");
    assertThat(httpClient.executeMethod(request), equalTo(SC_INTERNAL_SERVER_ERROR));
  }


  @Override
  protected HttpMethodBase createRequest(String uri) {
    return new PostMethod(uri);
  }
}
