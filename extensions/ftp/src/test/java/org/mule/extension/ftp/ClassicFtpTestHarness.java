/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.extension.AbstractFtpTestHarness;
import org.mule.extension.FtpTestHarness;
import org.mule.extension.ftp.api.FtpFileAttributes;
import org.mule.extension.ftp.api.ftp.ClassicFtpFileAttributes;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.infrastructure.client.ftp.FTPTestClient;
import org.mule.test.infrastructure.process.rules.FtpServer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.rules.TestRule;

/**
 * Implementation of {@link FtpTestHarness} for classic FTP connections
 *
 * @since 4.0
 */
public class ClassicFtpTestHarness extends AbstractFtpTestHarness {

  private static final String FTP_USER = "anonymous";
  private static final String FTP_PASSWORD = "password";

  private FtpServer ftpServer = new FtpServer("ftpPort", new File(FTP_SERVER_BASE_DIR, WORKING_DIR));
  private SystemProperty workingDirSystemProperty = new SystemProperty(WORKING_DIR_SYSTEM_PROPERTY, WORKING_DIR);
  private FTPTestClient ftpClient;


  /**
   * Creates a new instance activating the {@code ftp} spring profile
   */
  public ClassicFtpTestHarness() {
    super("ftp");
  }

  /**
   * Starts a FTP server and connects a client to it
   */
  @Override
  protected void doBefore() throws Exception {
    ftpServer.start();
    ftpClient = new FTPTestClient(DEFAULT_FTP_HOST, ftpServer.getPort(), FTP_USER, FTP_PASSWORD);

    if (!ftpClient.testConnection()) {
      throw new IOException("could not connect to ftp server");
    }
    ftpClient.changeWorkingDirectory(WORKING_DIR);
  }

  /**
   * Disconnects the client and shuts the server down
   */
  @Override
  protected void doAfter() throws Exception {
    try {
      if (ftpClient.isConnected()) {
        ftpClient.disconnect();
      }
    } finally {
      ftpServer.stop();
    }

  }

  /**
   * @return {@link #workingDirSystemProperty , and {@link #ftpServer}}
   */
  @Override
  protected TestRule[] getChildRules() {
    return new TestRule[] {workingDirSystemProperty, ftpServer};
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createHelloWorldFile() throws Exception {
    ftpClient.makeDir("files");
    ftpClient.putFile("files/" + HELLO_FILE_NAME, HELLO_WORLD);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createBinaryFile() throws Exception {
    ftpClient.putFile(BINARY_FILE_NAME, HELLO_WORLD.getBytes());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void makeDir(String directoryPath) throws Exception {
    ftpClient.makeDir(directoryPath);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getWorkingDirectory() throws Exception {
    return ftpClient.getWorkingDirectory();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(String path, String content) throws Exception {
    ftpClient.putFile(path, content);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean dirExists(String path) throws Exception {
    return ftpClient.dirExists(path);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean fileExists(String path) throws Exception {
    return ftpClient.fileExists(path);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean changeWorkingDirectory(String path) throws Exception {
    return ftpClient.changeWorkingDirectory(path);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String[] getFileList(String path) throws Exception {
    return ftpClient.getFileList(path);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void assertAttributes(String path, FtpFileAttributes fileAttributes) throws Exception {
    FTPFile file = ftpClient.get(path);

    assertThat(fileAttributes.getName(), equalTo(file.getName()));
    assertThat(fileAttributes.getPath(), equalTo(Paths.get("/", WORKING_DIR, HELLO_PATH).toString()));
    assertThat(fileAttributes.getSize(), is(file.getSize()));
    assertTime(fileAttributes.getTimestamp(), file.getTimestamp());
    assertThat(fileAttributes.isDirectory(), is(false));
    assertThat(fileAttributes.isSymbolicLink(), is(false));
    assertThat(fileAttributes.isRegularFile(), is(true));
  }

  private void assertTime(LocalDateTime dateTime, Calendar calendar) {
    assertThat(dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), is(calendar.toInstant().toEpochMilli()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void assertDeleted(String path) throws Exception {
    assertThat(fileExists(path), is(false));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class getAttributesType() {
    return ClassicFtpFileAttributes.class;
  }
}
