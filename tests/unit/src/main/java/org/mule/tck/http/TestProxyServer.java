/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.http;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of an HTTP proxy server for testing purposes. The server will accept many connections, which are expected to
 * send a CONNECT request if they are HTTPS in which case the request is consumed and a 200 OK answer returned, then it acts as a
 * tunnel between the client and the provided localhost target port. If stopped it will close active threads and
 * connectionHandlers.
 */
public class TestProxyServer {

  private static final String PROXY_RESPONSE = "HTTP/1.1 200 Connection established\r\n\r\n";
  private static final Logger logger = LoggerFactory.getLogger(TestProxyServer.class);

  private int listenPort;
  private int targetPort;
  private boolean https;
  private String targetHost;
  private ServerSocket serverSocket;
  private Thread serverThread;
  ArrayList<Thread> connectionHandlers;

  public TestProxyServer(int listenPort, int targetPort, boolean https) {
    this.listenPort = listenPort;
    this.targetPort = targetPort;
    this.https = https;
    this.targetHost = "localhost";
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
                  logger.warn("handling request: {}", e.getMessage());
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

  private void handleRequest(final Socket clientSocket) throws Exception {
    if (https) {
      // Look for CONNECT to consume and answer
      BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()), 1);

      String header;
      do {
        header = reader.readLine().trim();
      } while (header.isEmpty());

      if (!header.toLowerCase().startsWith("connect ")) {
        throw new RuntimeException(format("Test HTTP proxy received wrong header: %s", header));
      }
      reader.readLine();

      OutputStream os = clientSocket.getOutputStream();
      os.write(PROXY_RESPONSE.getBytes());
      os.flush();
    }

    // Make a tunnel between both sockets (HTTP traffic)
    final Socket targetSocket = new Socket(targetHost, targetPort);

    Thread requestThread = new Thread() {

      @Override
      public void run() {
        try {
          IOUtils.copy(clientSocket.getInputStream(), targetSocket.getOutputStream());
        } catch (IOException e) {
          logger.warn("requestThread: {}", e.getMessage());
        }
      }
    };

    Thread responseThread = new Thread(() -> {
      try {
        IOUtils.copy(targetSocket.getInputStream(), clientSocket.getOutputStream());
      } catch (IOException e) {
        logger.warn("responseThread: {}", e.getMessage());
      }
    });

    try {
      requestThread.start();
      responseThread.start();

      requestThread.join();
      responseThread.join();
    } catch (InterruptedException e) {
      // stop execution when interrupted from parent
    } finally {
      targetSocket.close();
      clientSocket.close();
    }
  }

  public boolean hasConnections() {
    return !connectionHandlers.isEmpty();
  }

}
