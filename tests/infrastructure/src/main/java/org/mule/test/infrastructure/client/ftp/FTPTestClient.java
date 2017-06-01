/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.client.ftp;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

/**
 * Ftp client wrapper for working with an FTP server.
 */
public class FTPTestClient {

  private static final int TIMEOUT = 5000;

  private FTPClient client = null;
  private String server = null;
  private int port;
  private String user = null;
  private String password = null;

  public FTPTestClient(String server, int port, String user, String password) {
    super();
    this.server = server;
    this.port = port;
    this.user = user;
    this.password = password;
    client = new FTPClient();
  }

  public boolean testConnection() throws IOException {
    connect();
    return verifyStatusCode(client.noop());
  }

  /**
   * Get a list of file names in a given directory for admin
   *
   * @return List of files/directories
   * @throws IOException
   */
  public String[] getFileList(String path) throws IOException {
    connect();
    return client.listNames(path);
  }

  public String getWorkingDirectory() throws IOException {
    return client.printWorkingDirectory();
  }

  /**
   * Create a directory
   *
   * @param dir
   * @return true if successful, false if not
   * @throws IOException
   */
  public boolean makeDir(String dir) throws IOException {
    connect();
    return verifyStatusCode(client.mkd(dir));
  }

  public FTPFile get(String path) throws IOException {
    FTPFile[] files = client.listFiles(path);
    if (files == null && files.length == 0) {
      throw new RuntimeException("Could not find file " + path);
    } else if (files.length > 1) {
      throw new RuntimeException("Too many matches for path " + path);
    }

    return files[0];
  }

  /**
   * Check that the status code is successful (between 200 and 299)
   *
   * @param status The status code to check
   * @return true if status is successful, false if not
   */
  private boolean verifyStatusCode(int status) {
    if (status >= 200 && status < 300) {
      return true;
    }
    return false;
  }

  /**
   * Upload a file to the ftp server
   *
   * @param path the path to write in
   * @param content the file's content
   * @return true if successful, false if not
   * @throws IOException
   */
  public boolean putFile(String path, String content) throws IOException {
    return putFile(path, content.getBytes());
  }

  /**
   * Upload a file to the ftp server
   *
   * @param path the path to write in
   * @param content the file's binary content
   * @return true if successful, false if not
   * @throws IOException
   */
  public boolean putFile(String path, byte[] content) throws IOException {
    connect();
    return client.storeFile(path, new ByteArrayInputStream(content));
  }

  /**
   * Check if a directory exists by trying to go to it
   *
   * @param path The directory to try
   * @return True if the directory exists, false if not
   * @throws IOException
   */
  public boolean dirExists(String path) throws IOException {
    connect();
    String cwd = client.printWorkingDirectory(); // store the current working dir so we can go back to it
    boolean dirExists = client.changeWorkingDirectory(path);
    client.changeWorkingDirectory(cwd); // go back to the cwd
    return dirExists;
  }

  /**
   * Initiate a connection to the ftp server
   *
   * @throws IOException
   */
  protected void connect() throws IOException {
    if (!client.isConnected()) {
      client = new FTPClient();
      client.setDefaultTimeout(TIMEOUT);
      client.connect(server, port);
      client.login(user, password);
    }
  }

  /**
   * Check if the ftp client is connected
   *
   * @return true if connected, false if not
   */
  public boolean isConnected() {
    return client.isConnected();
  }

  /**
   * Disconnect the ftp client
   *
   * @throws IOException
   */
  public void disconnect() throws IOException {
    client.disconnect();
  }

  /**
   * Check if a file exists on the ftp server
   *
   * @param file The name of the file to check
   * @return true if file exists, false if not
   * @throws IOException
   */
  public boolean fileExists(String file) throws IOException {
    return client.listFiles(file).length > 0;
  }


  public boolean changeWorkingDirectory(String path) throws Exception {
    return client.changeWorkingDirectory(path);
  }
}
