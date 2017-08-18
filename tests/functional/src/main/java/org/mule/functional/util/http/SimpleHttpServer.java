/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.util.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.LinkedList;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Simple HTTP server implementation for testing purposes.
 * 
 * @since 4.0
 */
public class SimpleHttpServer {

  public static final String DEFAULT_RESPONSE = "This is the response";
  private final int port;
  private HttpServer server;
  private LinkedList<HttpMessage> httpRequests = new LinkedList<>();
  private int statusCode = 200;

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
    try {
      server = HttpServer.create(new InetSocketAddress(port), 0);
      server.createContext("/", new TestHandler());
      server.setExecutor(null);
      server.start();
      return this;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Stops the http server.
   */
  public void stop() {
    try {
      server.stop(0);
    } catch (Exception e) {
      // nothing to do.
    }
  }

  class TestHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      httpRequests.addLast(new HttpMessage(exchange));
      String response = DEFAULT_RESPONSE;
      exchange.sendResponseHeaders(statusCode, response.length());
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }

  public void setResponseStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }

}
