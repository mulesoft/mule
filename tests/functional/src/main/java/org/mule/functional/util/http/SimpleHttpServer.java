/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.util.http;

import org.mule.runtime.api.exception.MuleRuntimeException;

import java.io.IOException;
import java.util.LinkedList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 * Simple HTTP server implementation for testing purposes.
 *
 * @since 4.0
 */
public class SimpleHttpServer {

  public static final String DEFAULT_RESPONSE = "This is the response";
  private final int port;
  private Server server;
  private final LinkedList<HttpMessage> httpRequests = new LinkedList<>();
  private int statusCode = 200;
  private long responseDelay = -1;

  public HttpMessage getLastHttpRequest() {
    return httpRequests.getLast();
  }

  public HttpMessage getHttpRequest(int index) {
    return httpRequests.get(index);
  }

  private SimpleHttpServer(int port) {
    this.port = port;
  }

  /**
   * @param port port to listen for http requests.
   * @return a new {@link SimpleHttpServer} created to listen in the port {@code port}
   */
  public static SimpleHttpServer createServer(int port) {
    SimpleHttpServer simpleHttpServer = new SimpleHttpServer(port);
    return simpleHttpServer;
  }

  /**
   * Starts the http server.
   *
   * @return {@code this}
   */
  public SimpleHttpServer start() {
    server = new Server(port);
    server.setHandler(new TestHandler());
    try {
      server.start();
      return this;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Stops the http server.
   */
  public void stop() {
    try {
      server.stop();
    } catch (Exception e) {
      // nothing to do.
    }
  }

  class TestHandler extends AbstractHandler {

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
        throws IOException {
      httpRequests.addLast(new HttpMessage(request));

      if (responseDelay > 0) {
        try {
          Thread.sleep(responseDelay);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new MuleRuntimeException(e);
        }
      }

      String responseBody = DEFAULT_RESPONSE;
      response.setStatus(statusCode);
      response.setContentType("text/plain");
      response.getWriter().write(responseBody);
      baseRequest.setHandled(true);
    }
  }

  public void setResponseStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }

  public void setResponseDelay(long responseDelay) {
    this.responseDelay = responseDelay;
  }
}
