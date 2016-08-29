/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.util.concurrent.Latch;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.junit.Test;

public class HttpRequestMaxConnectionsTestCase extends AbstractHttpRequestTestCase {

  private Latch messageArrived = new Latch();
  private Latch messageHold = new Latch();

  @Override
  protected String getConfigFile() {
    return "http-request-max-connections-config.xml";
  }

  @Test
  public void maxConnections() throws Exception {
    Flow flow = (Flow) getFlowConstruct("limitedConnections");
    Thread t1 = processAsynchronously(flow);
    messageArrived.await();

    MessagingException e = flowRunner("limitedConnections").runExpectingException();
    // Max connections should be reached
    assertThat(e, instanceOf(MessagingException.class));
    assertThat(e.getCause(), instanceOf(IOException.class));

    messageHold.release();
    t1.join();
  }

  private Thread processAsynchronously(final Flow flow) {
    Thread thread = new Thread(() -> {
      try {
        flowRunner("limitedConnections").run();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    thread.start();
    return thread;
  }

  @Override
  protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
    super.handleRequest(baseRequest, request, response);
    messageArrived.release();
    try {
      messageHold.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

}
