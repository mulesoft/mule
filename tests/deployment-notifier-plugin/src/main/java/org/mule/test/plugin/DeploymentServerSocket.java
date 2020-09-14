/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.plugin;

import static java.lang.String.format;
import static java.net.InetAddress.getByName;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;

/**
 * Deployment Server Socket responsible to answer deployment's client about app and domains
 * deployment status
 *
 * @since 4.0
 */
public class DeploymentServerSocket {

  private static final Logger LOGGER = getLogger(DeploymentServerSocket.class);

  private static final String DEPLOYED_APP_PREFIX = "DEPLOYED_APP";
  private static final String DEPLOYED_DOMAIN_PREFIX = "DEPLOYED_DOMAIN";
  private static final String COMMAND_SEPARATOR = "!";

  private final int port;
  private final DeploymentNotifierPlugin plugin;

  private ServerSocket serverSocket;
  private Thread serverThread;
  private List<Thread> connectionHandlers = new ArrayList<>();

  public DeploymentServerSocket(int port, DeploymentNotifierPlugin plugin) {
    this.port = port;
    this.plugin = plugin;
  }

  public void start() throws IOException {
    LOGGER.info(format("Trying to create deployment server socket at port:%s", port));
    serverSocket = new ServerSocket(port, 0, getByName("localhost"));
    serverThread = new Thread("Deployment-Server") {

      @Override
      public void run() {
        try {
          LOGGER.info(format("Running deployment server socket at port:%s", port));
          while (true) {
            final Socket clientSocket = serverSocket.accept();
            LOGGER.info("New client connected ...");
            Thread handlerThread = new RequestHandler(clientSocket);
            connectionHandlers.add(handlerThread);
            handlerThread.start();
          }
        } catch (SocketException e) {
          // stop execution when closed from parent
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    };
    serverThread.start();
  }

  public void stop() {
    LOGGER.info("Stopping deployment server socket ...");
    closeQuietly(serverSocket);
    try {
      if (serverThread != null) {
        serverThread.join();
      }
    } catch (Exception e) {
      LOGGER.warn("Deployment server socket fail stopping", e);
    }
    for (Thread c : connectionHandlers) {
      try {
        c.interrupt();
        c.join();
      } catch (Exception e) {
        LOGGER.warn("Deployment server socket fail stopping", e);
      }
    }
    LOGGER.info("Stopped deployment server socket");
  }

  private void closeQuietly(ServerSocket serverSocket) {
    try {
      if (serverSocket != null) {
        serverSocket.close();
      }
    } catch (Exception e) {
      // Do nothing.
      LOGGER.warn("Fail closing deployment server socket", e);
    }
  }

  class RequestHandler extends Thread {

    private final Socket socket;

    RequestHandler(Socket socket) {
      this.socket = socket;
    }

    @Override
    public void run() {
      try {
        LOGGER.info("Received a connection");
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        String line = in.readLine();
        while (line != null && line.length() > 0) {
          LOGGER.debug(format("Received request: %s", line));
          boolean response = processRequest(line);
          LOGGER.debug(format("Sending response: %s", response));
          out.println(response);
          line = in.readLine();
        }
        socket.close();
        LOGGER.info("Connection closed");
      } catch (Exception e) {
        LOGGER.warn("Deployment server socket failed closing socket", e);
      }
    }

    private boolean processRequest(String request) {
      String[] command = request.split(COMMAND_SEPARATOR);
      if (command.length != 2) {
        LOGGER.warn(format("Unsupported request: %s", request));
        return false;
      }

      String artifactName = command[1];
      boolean isDeployed = false;
      switch (command[0]) {
        case DEPLOYED_APP_PREFIX:
          isDeployed = plugin.isDeployed(artifactName);
          break;
        case DEPLOYED_DOMAIN_PREFIX:
          isDeployed = plugin.isDomainDeployed(artifactName);
          break;
        default:
          LOGGER.warn(format("Unexpected value: %s", command[0]));
      }

      return isDeployed;
    }
  }
}
