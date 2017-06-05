/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.server.ftp;

import org.mule.runtime.core.api.util.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;

/**
 * A wrapper for the Apache ftpServer. This will progress into a provider of its own, but for now is necessary to avoid
 * duplicating code in FTP tests using FTPClient.
 */
public class EmbeddedFtpServer {

  private org.apache.ftpserver.FtpServer server;
  private final int port;

  /**
   * Initialize the ftp server on a given port
   *
   * @param port The port to start the server on. Note, you need special permissions on *nux to open port 22, so we usually choose
   *        a very high port number.
   * @throws Exception
   */
  public EmbeddedFtpServer(int port) throws Exception {
    this.port = port;
  }

  private Listener createListener(int port) {
    ListenerFactory listenerFactory = createListenerFactory(port);

    return listenerFactory.createListener();
  }

  protected ListenerFactory createListenerFactory(int port) {
    ListenerFactory listenerFactory = new ListenerFactory();

    listenerFactory.setPort(port);
    listenerFactory.setIdleTimeout(60000);
    return listenerFactory;
  }

  private void setupUserManagerFactory(FtpServerFactory serverFactory) throws IOException, URISyntaxException {
    PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
    URL usersFile = IOUtils.getResourceAsUrl("users.properties", getClass());
    if (usersFile == null) {
      throw new IOException("users.properties file not found in the classpath");
    }
    userManagerFactory.setFile(new File(usersFile.toURI()));
    serverFactory.setUserManager(userManagerFactory.createUserManager());
  }

  public void start() {
    FtpServerFactory serverFactory = new FtpServerFactory();
    serverFactory.addListener("default", createListener(port));
    try {
      setupUserManagerFactory(serverFactory);
      server = serverFactory.createServer();
      server.start();
    } catch (Exception e) {
      throw new RuntimeException("Could not start server", e);
    }
  }

  /**
   * Stop the ftp server TODO DZ: we may want to put a port check + wait time in here to make sure that the port is released
   * before we continue. Windows tends to hold on to ports longer than it should.
   */
  public void stop() {
    if (server != null) {
      server.stop();
    }
  }
}
