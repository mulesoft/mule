/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.TeeInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of an HTTP proxy server for testing purposes. The server will accept many connections,
 * which are expected to send a CONNECT request. The request is consumed, a 200 OK answer is returned, and then
 * it acts as a tunnel between the client and the provided localhost target port. If stopped will close active
 * threads and connectionHandlers.
 *
 * Optionally provides traffic inspection, host redirection and HTTP header version string selection.
 */
public class TestProxyServer {

  private static final String PROXY_RESPONSE = "HTTP/%s 200 Connection established\r\n\r\n";
  private static final Logger logger = LoggerFactory.getLogger(TestProxyServer.class);

  private int listenPort;
  private int targetPort;
  private String targetHost;
  private ServerSocket serverSocket;
  private Thread serverThread;
  private String httpVersion;
  private boolean viewTraffic;
  ArrayList<Thread> connectionHandlers;

  public TestProxyServer(int listenPort, int targetPort) {
    this.listenPort = listenPort;
    this.targetPort = targetPort;
    this.targetHost = "localhost";
    this.httpVersion = "1.1";
    this.viewTraffic = false;
    this.connectionHandlers = new ArrayList<>();
  }

  public void start() throws Exception {
    serverSocket = new ServerSocket(listenPort);

    serverThread = new Thread(new Runnable() {

      @Override
      public void run() {
        try {
          while (true) {
            final Socket clientSocket = serverSocket.accept();
            Thread handlerThread = new Thread(new Runnable() {

              @Override
              public void run() {
                try {
                  handleRequest(clientSocket);
                } catch (Exception e) {
                  // ignore exception
                }
              }
            });
            connectionHandlers.add(handlerThread);
            handlerThread.start();
          }
        } catch (SocketException e) {
          // stop execution when closed from parent
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    });

    serverThread.start();
  }

  public void stop() throws Exception {
    serverSocket.close();
    serverThread.join();

    for (Thread c : connectionHandlers) {
      c.interrupt();
      c.join();
    }
  }

  private InputStream tapIntoStream(InputStream is) {
    return this.viewTraffic ? new TeeInputStream(is, System.out) : is;
  }

  private void handleRequest(final Socket clientSocket) throws Exception {
    BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()), 1);

    String header;
    do {
      // skip blank lines
      header = reader.readLine().trim();
    } while (header.isEmpty());

    // Consume the CONNECT request
    if (!header.toLowerCase().startsWith("connect ")) {
      throw new RuntimeException(format("Test HTTP proxy received wrong header: %s", header));
    }
    reader.readLine();

    OutputStream os = clientSocket.getOutputStream();
    os.write(format(PROXY_RESPONSE, httpVersion).getBytes());
    os.flush();

    // Make a tunnel between both sockets (HTTP traffic)
    final Socket targetSocket = new Socket(targetHost, targetPort);

    Thread responseThread = new Thread() {

      @Override
      public void run() {
        try {
          IOUtils.copy(tapIntoStream(targetSocket.getInputStream()), clientSocket.getOutputStream());
        } catch (IOException e) {
          logger.warn("responseThread: {}", e.getMessage());
        }
      }
    };

    Thread requestThread = new Thread() {

      @Override
      public void run() {
        try {
          IOUtils.copy(tapIntoStream(clientSocket.getInputStream()), targetSocket.getOutputStream());
        } catch (IOException e) {
          logger.warn("requestThread: {}", e.getMessage());
        }
      }
    };

    try {
      responseThread.start();
      requestThread.start();

      responseThread.join();
      requestThread.join();
    } catch (InterruptedException e) {
      // stop execution when interrupted from parent
    } finally {
      targetSocket.close();
      clientSocket.close();
    }
  }

  public void setHttpVersion(String httpVersion) {
    this.httpVersion = httpVersion;
  }

  public void setViewTraffic(boolean viewTraffic) {
    this.viewTraffic = viewTraffic;
  }

  public void setTargetHost(String targetHost) {
    this.targetHost = targetHost;
  }
}
