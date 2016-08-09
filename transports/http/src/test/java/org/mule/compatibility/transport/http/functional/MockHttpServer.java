/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.compatibility.transport.http.functional;

import org.mule.compatibility.transport.http.HttpRequest;
import org.mule.compatibility.transport.http.RequestLine;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.HttpParser;

public abstract class MockHttpServer extends Object implements Runnable {

  private static final long MOCK_HTTP_SERVER_TIMEOUT = 10000;

  public static final String HTTP_STATUS_LINE_OK = "HTTP/1.1 200 OK\n";

  private final int listenPort;
  private final CountDownLatch startupLatch = new CountDownLatch(1);
  private final CountDownLatch shutdownLatch = new CountDownLatch(1);

  public MockHttpServer(int listenPort) {
    this.listenPort = listenPort;
  }

  protected abstract void processRequests(InputStream in, OutputStream out) throws Exception;

  @Override
  public void run() {
    try {
      ServerSocket serverSocket = new ServerSocket(listenPort);
      startupLatch.countDown();

      Socket clientSocket = serverSocket.accept();

      InputStream in = clientSocket.getInputStream();
      OutputStream out = clientSocket.getOutputStream();

      processRequests(in, out);

      in.close();
      out.close();
      clientSocket.close();
      serverSocket.close();

      shutdownLatch.countDown();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  protected HttpRequest parseRequest(InputStream in, Charset encoding) {
    try {
      String line = HttpParser.readLine(in, encoding.name());
      RequestLine requestLine = RequestLine.parseLine(line);

      return new HttpRequest(requestLine, HttpParser.parseHeaders(in, encoding.name()), in, encoding);

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public boolean waitUntilServerStarted() throws Exception {
    return startupLatch.await(MOCK_HTTP_SERVER_TIMEOUT, TimeUnit.MILLISECONDS);
  }

  public boolean waitUntilServerStopped() throws Exception {
    return shutdownLatch.await(MOCK_HTTP_SERVER_TIMEOUT, TimeUnit.MILLISECONDS);
  }
}


