/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.components;

import static org.junit.Assert.assertTrue;
import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;

import org.mule.compatibility.transport.http.HttpConstants;
import org.mule.compatibility.transport.http.HttpRequest;
import org.mule.compatibility.transport.http.functional.AbstractMockHttpServerTestCase;
import org.mule.compatibility.transport.http.functional.MockHttpServer;
import org.mule.compatibility.transport.http.functional.SingleRequestMockHttpServer;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;

public class RestServiceComponentDeleteTestCase extends AbstractMockHttpServerTestCase {

  private CountDownLatch serverRequestCompleteLatch = new CountDownLatch(1);
  private boolean deleteRequestFound = false;

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "rest-service-component-delete-test-flow.xml";
  }

  @Override
  protected MockHttpServer getHttpServer() {
    return new SimpleHttpServer(dynamicPort.getNumber());
  }

  @Test
  public void testRestServiceComponentDelete() throws Exception {
    MuleClient client = muleContext.getClient();
    client.send("vm://fromTest", TEST_MESSAGE, null);

    assertTrue(serverRequestCompleteLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
    assertTrue(deleteRequestFound);
  }

  private class SimpleHttpServer extends SingleRequestMockHttpServer {

    public SimpleHttpServer(int listenPort) {
      super(listenPort, getDefaultEncoding(muleContext));
    }

    @Override
    protected void processSingleRequest(HttpRequest httpRequest) throws Exception {
      deleteRequestFound = httpRequest.getRequestLine().getMethod().equals(HttpConstants.METHOD_DELETE);
      serverRequestCompleteLatch.countDown();
    }
  }
}
