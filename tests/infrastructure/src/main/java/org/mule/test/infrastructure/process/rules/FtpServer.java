/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process.rules;

import org.mule.runtime.core.api.util.FileUtils;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.infrastructure.server.ftp.EmbeddedFtpServer;

import java.io.File;

import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A {@link TestRule} which starts an {@link EmbeddedFtpServer}.
 * <p>
 * It automatically allocates a dynamic port and exposes the selected port on a system property under a configurable key.
 *
 * @since 4.0
 */
public class FtpServer extends ExternalResource {

  private final File baseDir;
  protected final DynamicPort dynamicPort;
  private final SystemProperty dynamicPortSystemProperty;

  private EmbeddedFtpServer server = null;

  /**
   * Creates a new instance
   *
   * @param ftpServerPortName the name of the system property on which the port will be exposed
   * @param baseDir the base dir for the FTP server
   */
  public FtpServer(String ftpServerPortName, File baseDir) {
    this.baseDir = baseDir;
    dynamicPort = new DynamicPort(ftpServerPortName + "_PORT");
    dynamicPortSystemProperty = new SystemProperty(ftpServerPortName, String.valueOf(dynamicPort.getNumber()));
  }

  @Override
  public Statement apply(Statement base, Description description) {
    base = dynamicPort.apply(base, description);
    base = dynamicPortSystemProperty.apply(base, description);
    return super.apply(base, description);
  }

  public void start() throws Exception {
    try {
      createFtpServerBaseDir();
      server = createServer();
      server.start();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  public void stop() {
    stopServer();
    deleteFtpServerBaseDir();
  }

  protected EmbeddedFtpServer createServer() throws Exception {
    return new EmbeddedFtpServer(dynamicPort.getNumber());
  }

  private void stopServer() {
    if (server != null) {
      try {
        server.stop();
      } catch (Exception e) {
        throw new RuntimeException("Could not stop FTP server", e);
      }
    }
  }

  private void createFtpServerBaseDir() {
    deleteFtpServerBaseDir();
    baseDir.mkdirs();
  }

  private void deleteFtpServerBaseDir() {
    FileUtils.deleteTree(baseDir);
  }

  /**
   * @return the port number on which the ftp server is listening
   */
  public int getPort() {
    return dynamicPort.getNumber();
  }
}
