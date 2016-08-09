/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.functional.util.sftp.SftpServer.PASSWORD;
import static org.mule.functional.util.sftp.SftpServer.USERNAME;
import static org.mule.runtime.module.extension.file.api.FileWriteMode.APPEND;
import static org.mule.runtime.module.extension.file.api.FileWriteMode.OVERWRITE;
import org.mule.extension.AbstractFtpTestHarness;
import org.mule.extension.FtpTestHarness;
import org.mule.extension.ftp.api.FtpFileAttributes;
import org.mule.extension.ftp.api.sftp.SftpFileAttributes;
import org.mule.extension.ftp.internal.sftp.connection.SftpClient;
import org.mule.extension.ftp.internal.sftp.connection.SftpClientFactory;
import org.mule.functional.util.sftp.SftpServer;
import org.mule.runtime.module.extension.file.api.FileAttributes;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;

/**
 * Implementation of {@link FtpTestHarness} for classic SFTP connections
 *
 * @since 4.0
 */
public class SftpTestHarness extends AbstractFtpTestHarness {

  private static final String SFTP_PORT = "SFTP_PORT";

  private TemporaryFolder temporaryFolder = new TemporaryFolder();
  private DynamicPort sftpPort = new DynamicPort(SFTP_PORT);
  private SftpServer sftpServer;
  private SftpClient sftpClient;

  /**
   * Creates a new instance which activates the {@code sftp} spring profile
   */
  public SftpTestHarness() {
    super("sftp");
  }

  /**
   * Starts a SFTP server and connects a client to it
   */
  @Override
  protected void doBefore() throws Exception {
    temporaryFolder.create();
    System.setProperty(WORKING_DIR_SYSTEM_PROPERTY, temporaryFolder.getRoot().getAbsolutePath());
    setUpServer();
    sftpClient = createDefaultSftpClient();
  }

  /**
   * Disconnects the client and shuts the server down
   */
  @Override
  protected void doAfter() throws Exception {
    try {
      if (sftpClient != null) {
        sftpClient.disconnect();
      }

      if (sftpServer != null) {
        sftpServer.stop();
      }
    } finally {
      temporaryFolder.delete();
      System.clearProperty(WORKING_DIR_SYSTEM_PROPERTY);
    }
  }

  private SftpClient createDefaultSftpClient() throws IOException {
    SftpClient sftpClient = new SftpClientFactory().createInstance("localhost", sftpPort.getNumber());
    sftpClient.setPassword(PASSWORD);
    sftpClient.login(USERNAME);
    sftpClient.changeWorkingDirectory(temporaryFolder.getRoot().getAbsolutePath());
    return sftpClient;
  }

  private void setUpServer() {
    sftpServer = new SftpServer(sftpPort.getNumber());
    sftpServer.start();
  }

  /**
   * @return {@link #sftpPort}
   */
  @Override
  protected TestRule[] getChildRules() {
    return new TestRule[] {sftpPort};
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createHelloWorldFile() throws Exception {
    final String dir = "files";
    makeDir(dir);
    write(dir, HELLO_FILE_NAME, HELLO_WORLD);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createBinaryFile() throws Exception {
    sftpClient.write(BINARY_FILE_NAME, new ByteArrayInputStream(HELLO_WORLD.getBytes()), OVERWRITE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void makeDir(String directoryPath) throws Exception {
    sftpClient.mkdir(directoryPath);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getWorkingDirectory() throws Exception {
    return sftpClient.getWorkingDirectory();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(String path, String content) throws Exception {
    sftpClient.write(path, new ByteArrayInputStream(content.getBytes()), APPEND);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean dirExists(String path) throws Exception {
    FileAttributes attributes = sftpClient.getAttributes(Paths.get(path));
    return attributes != null && attributes.isDirectory();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean fileExists(String path) throws Exception {
    return sftpClient.getAttributes(Paths.get(path)) != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean changeWorkingDirectory(String path) throws Exception {
    try {
      sftpClient.changeWorkingDirectory(path);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String[] getFileList(String path) throws Exception {
    List<String> files = sftpClient.list(path).stream().map(FileAttributes::getPath).collect(toList());
    return files.toArray(new String[files.size()]);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void assertAttributes(String path, FtpFileAttributes fileAttributes) throws Exception {
    SftpFileAttributes file = sftpClient.getAttributes(Paths.get(path));

    assertThat(fileAttributes.getName(), equalTo(file.getName()));

    assertThat(fileAttributes.getPath(), equalTo(Paths.get(temporaryFolder.getRoot().getPath(), HELLO_PATH).toString()));
    assertThat(fileAttributes.getSize(), is(file.getSize()));
    assertThat(fileAttributes.getTimestamp(), equalTo(file.getTimestamp()));
    assertThat(fileAttributes.isDirectory(), is(false));
    assertThat(fileAttributes.isSymbolicLink(), is(false));
    assertThat(fileAttributes.isRegularFile(), is(true));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void assertDeleted(String path) throws Exception {
    Path directoryPath = temporaryFolder.getRoot().toPath().resolve(path);
    int lastFragmentIndex = directoryPath.getNameCount() - 1;

    Path lastFragment = directoryPath.getName(lastFragmentIndex);
    if (".".equals(lastFragment.toString())) {
      directoryPath = Paths.get("/").resolve(directoryPath.subpath(0, lastFragmentIndex)).toAbsolutePath();
    }

    assertThat(dirExists(directoryPath.toString()), is(false));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class getAttributesType() {
    return SftpFileAttributes.class;
  }
}
