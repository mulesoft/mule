/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process;

import org.mule.runtime.api.util.concurrent.Latch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestProcess implements CommandServer.CommandListener {

  private Logger logger = LoggerFactory.getLogger(TestProcess.class);

  private final Latch clientSocketAcceptedLatch = new Latch();
  private final Latch contextStartedLatch = new Latch();
  private Process process;
  private String instanceId;
  private ServerSocket loggerSocket;
  private CommandServer commandServer;
  private Thread loggerThread;

  public TestProcess(String instanceId, int loggerServerPort, int commandServerPort) {
    this.instanceId = instanceId;
    createLoggerServerSocket(loggerServerPort);
    createCommandServer(commandServerPort);
  }

  public void destroyQuietly() {
    try {
      logger.info(String.format("# Destroying process for instance %s #", instanceId));
      loggerThread.interrupt();
      closeQuietly(loggerSocket);
      commandServer.stop();
      process.destroy();
    } catch (Exception e) {
      // Nothing to do.
    }
  }

  @Override
  public void commandReceived(String command) {
    if (command.equals(CommandServer.MULE_CONTEXT_STARTED_COMMAND)) {
      contextStartedLatch.release();
    }
  }

  public void waitUntilStarted() throws InterruptedException {
    waitUntilStarted(30);
  }

  public void waitUntilStarted(int contextStartedTiemout) throws InterruptedException {
    if (!clientSocketAcceptedLatch.await(TestUtils.getTimeout(20), TimeUnit.SECONDS)) {
      throw new IllegalStateException(String.format("Client process %s didn not connect connect to logger service", instanceId));
    }
    if (!contextStartedLatch.await(TestUtils.getTimeout(contextStartedTiemout), TimeUnit.SECONDS)) {
      throw new RuntimeException(String.format("Process %s didn not start within 30 seconds", instanceId));
    }
  }

  private void createCommandServer(int commandServerPort) {
    try {
      logger.info("Creating command server on port: " + commandServerPort + " for instance " + instanceId);
      commandServer = new CommandServer(commandServerPort);
      commandServer.start();
      commandServer.setCommandListener(this);
    } catch (IOException e) {
      if (commandServer != null) {
        commandServer.stop();
      }
      throw new RuntimeException(e);
    }
  }

  private void createLoggerServerSocket(int loggerServerPort) {
    try {
      logger.info("Creating logger service on port: " + loggerServerPort);
      loggerSocket = new ServerSocket(loggerServerPort, 0, InetAddress.getByName("localhost"));
      loggerThread = new Thread(String.format("Process %s logger", instanceId)) {

        @Override
        public void run() {
          try {
            Socket processClientConnection = loggerSocket.accept();
            clientSocketAcceptedLatch.release();
            BufferedReader processClientLogEntriesInputStream =
                new BufferedReader(new InputStreamReader(processClientConnection.getInputStream()));
            while (!Thread.interrupted()) {
              String logLine = processClientLogEntriesInputStream.readLine();
              if (logLine == null) {
                try {
                  Thread.sleep(200);
                } catch (InterruptedException e) {
                  return;
                }
              } else {
                logger.info(logLine);
              }
            }
          } catch (IOException e) {
            throw new RuntimeException(e);
          } finally {
            closeQuietly(loggerSocket);
          }
        }
      };
      loggerThread.setDaemon(true);
      loggerThread.start();
    } catch (IOException e) {
      closeQuietly(loggerSocket);
      throw new RuntimeException(e);
    }
  }

  private void closeQuietly(ServerSocket loggerSocket) {
    try {
      if (loggerSocket != null) {
        loggerSocket.close();
      }
    } catch (IOException e) {
      // Do nothing.
    }
  }

  public void setProcess(Process process) {
    this.process = process;
  }
}
