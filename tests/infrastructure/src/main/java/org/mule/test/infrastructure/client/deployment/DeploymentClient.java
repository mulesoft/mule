/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.client.deployment;

import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Deployment client to {@link DeploymentServerSocket} server.
 *
 * @since 4.0
 */
public class DeploymentClient {

  private static final Logger LOGGER = getLogger(DeploymentClient.class);
  private static final String DEPLOYED_APP = "DEPLOYED_APP";
  private static final String DEPLOYED_DOMAIN = "DEPLOYED_DOMAIN";
  private static final String COMMAND_SEPARATOR = "!";

  private final String hostname;
  private final int port;
  private Socket socket;

  public DeploymentClient(int port) {
    this("localhost", port);
  }

  public DeploymentClient(String hostname, int port) {
    this.hostname = hostname;
    this.port = port;
  }

  public void start() throws IOException {
    LOGGER.info(format("Starting deployment-client on %s:%s ...", hostname, port));
    socket = new Socket(hostname, port);
    LOGGER.info(format("Started deployment-client on %s:%s", hostname, port));
  }

  public boolean isDeployed(String artifactName) throws IOException {
    sendRequest(socket, DEPLOYED_APP + COMMAND_SEPARATOR + artifactName);
    String response = getResponse(socket);
    return parseResponse(response);
  }

  private void sendRequest(Socket socket, String request) throws IOException {
    LOGGER.info(format("Sending request: [%s] ...", request));
    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
    out.println(request);
  }

  private String getResponse(Socket socket) {
    LOGGER.info("Server response ....");
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      return in.readLine();
    } catch (IOException e) {
      return null;
    }
  }

  private boolean parseResponse(String response) {
    LOGGER.info(format("Server response: [%s]", response));
    return parseBoolean(response);
  }

  public boolean isDomainDeployed(String artifactName) throws IOException {
    sendRequest(socket, DEPLOYED_DOMAIN + COMMAND_SEPARATOR + artifactName);
    String response = getResponse(socket);
    return parseResponse(response);
  }

  public void stop() {
    if (socket != null) {
      closeQuietly(socket);
    }
  }

  private void closeQuietly(Socket socket) {
    try {
      socket.close();
    } catch (IOException e) {
      // Do nothing.
      LOGGER.warn("Fail closing deployment client socket", e);
    }
  }
}
