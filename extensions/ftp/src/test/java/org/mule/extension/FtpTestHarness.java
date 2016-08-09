/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension;

import org.mule.extension.ftp.api.FtpFileAttributes;

import org.junit.rules.ExpectedException;
import org.junit.rules.TestRule;

/**
 * A {@link TestRule} which provides tools and environment for reusing the same test regardless of the remote server type (FTP,
 * SFTP, etc) or the authentication method
 *
 * @since 4.0
 */
public interface FtpTestHarness extends TestRule {

  String HELLO_WORLD = "Hello World!";
  String HELLO_FILE_NAME = "hello.json";
  String BINARY_FILE_NAME = "binary.bin";
  String HELLO_PATH = "files/" + HELLO_FILE_NAME;
  String DEFAULT_FTP_HOST = "localhost";
  String FTP_SERVER_BASE_DIR = "target/ftpserver";
  String WORKING_DIR_SYSTEM_PROPERTY = "workingDir";
  String WORKING_DIR = "base";

  /**
   * Creates a test hello world file
   */
  void createHelloWorldFile() throws Exception;

  /**
   * Creates a test binary file
   */
  void createBinaryFile() throws Exception;

  /**
   * @return the {@link ExpectedException} rule used for the current test
   */
  ExpectedException expectedException();

  /**
   * Creates a directory
   *
   * @param directoryPath the path to the directory to be created
   */
  void makeDir(String directoryPath) throws Exception;

  /**
   * @return the current working directory
   */
  String getWorkingDirectory() throws Exception;

  /**
   * Writes the {@code content} into the given path
   *
   * @param folder the path to the target folder
   * @param fileName the name of the target file
   * @param content the content to be written
   */
  void write(String folder, String fileName, String content) throws Exception;

  /**
   * Writes the {@code content} into the given {@code path}
   *
   * @param path the path to write into
   * @param content the content to be written
   */
  void write(String path, String content) throws Exception;

  /**
   * @param path the path to test
   * @return whether the given {@code path} exists and it's a directory
   */
  boolean dirExists(String path) throws Exception;

  /**
   * @param path the path to test
   * @return whether the given {@code path} exists and it's a file
   */
  boolean fileExists(String path) throws Exception;

  boolean changeWorkingDirectory(String path) throws Exception;

  /**
   * Lists the file paths of a given directory
   *
   * @param path the path of the directory to test
   * @return the paths of the files contained in the given {@code path}
   */
  String[] getFileList(String path) throws Exception;

  /**
   * Performs that the file at the given {@code path} is consistent with the expected {@code attributes}
   *
   * @param path the path to test
   * @param attributes the expected attributes
   */
  void assertAttributes(String path, FtpFileAttributes attributes) throws Exception;

  /**
   * Validates that the given {@code path} has been deleted
   *
   * @param path the path to test
   */
  void assertDeleted(String path) throws Exception;

  /**
   * @return the Class of the attributes that works with the specified file system.
   */
  Class getAttributesType();
}
