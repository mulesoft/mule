/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandServer {

  public static final String MULE_CONTEXT_STARTED_COMMAND = "MuleContext:started";
  private final Logger logger = LoggerFactory.getLogger(CommandServer.class);
  private final int serverPort;

  private ServerSocket commandSocket;
  private List<String> pendingCommands = new ArrayList<String>();
  private CommandListener commandListener;
  private Thread commandServerThread;

  public CommandServer(int serverPort) {
    this.serverPort = serverPort;
  }

  public synchronized void setCommandListener(CommandListener commandListener) {
    this.commandListener = commandListener;
    for (String command : pendingCommands) {
      this.commandListener.commandReceived(command);
    }
  }

  public void start() throws IOException {
    logger.debug("Trying to create server socket for command service to port: " + serverPort);
    commandSocket = new ServerSocket(serverPort, 0, InetAddress.getByName("localhost"));
    commandServerThread = new Thread("Command-Server") {

      @Override
      public void run() {
        try {
          Socket processClientConnection = commandSocket.accept();
          BufferedReader processClientLogEntriesInputStream =
              new BufferedReader(new InputStreamReader(processClientConnection.getInputStream()));
          while (!Thread.interrupted()) {
            String commandLine = processClientLogEntriesInputStream.readLine();
            if (commandLine == null) {
              try {
                Thread.sleep(200);
              } catch (InterruptedException e) {
                return;
              }
            } else {
              logger.info("Command from external process received: " + commandLine);
              synchronized (this) {
                if (commandListener == null) {
                  pendingCommands.add(commandLine);
                } else {
                  commandListener.commandReceived(commandLine);
                }
              }
            }
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        } finally {
          closeQuietly(commandSocket);
        }
      }
    };
    commandServerThread.setDaemon(true);
    commandServerThread.start();
  }

  private void closeQuietly(ServerSocket loggerSocket) {
    try {
      if (loggerSocket != null) {
        loggerSocket.close();
      }
    } catch (Exception e) {
      // Do nothing.
    }
  }

  public void stop() {
    closeQuietly(commandSocket);
    if (commandServerThread != null) {
      commandServerThread.interrupt();
    }
  }

  public interface CommandListener {

    void commandReceived(String command);

  }
}
