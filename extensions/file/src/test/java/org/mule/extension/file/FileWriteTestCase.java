/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file;

import static org.mule.extension.file.common.api.FileWriteMode.APPEND;
import static org.mule.extension.file.common.api.FileWriteMode.CREATE_NEW;
import static org.mule.extension.file.common.api.FileWriteMode.OVERWRITE;
import static org.mule.extension.file.common.api.exceptions.FileError.FILE_ALREADY_EXISTS;
import static org.mule.extension.file.common.api.exceptions.FileError.ILLEGAL_PATH;
import static org.mule.test.allure.AllureConstants.FileFeature.FILE_EXTENSION;
import static java.nio.charset.Charset.availableCharsets;
import static org.apache.commons.io.FileUtils.readFileToByteArray;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.extension.file.common.api.FileWriteMode;
import org.mule.extension.file.common.api.exceptions.FileAlreadyExistsException;
import org.mule.extension.file.common.api.exceptions.IllegalPathException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.util.FileUtils;

import java.io.File;
import java.util.Arrays;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;

@Features(FILE_EXTENSION)
public class FileWriteTestCase extends FileConnectorTestCase {

  private static final String TEST_FILENAME = "test.txt";

  @Override
  protected String getConfigFile() {
    return "file-write-config.xml";
  }

  @Test
  public void appendOnNotExistingFile() throws Exception {
    doWriteOnNotExistingFile(APPEND);
  }

  @Test
  public void overwriteOnNotExistingFile() throws Exception {
    doWriteOnNotExistingFile(OVERWRITE);
  }

  @Test
  public void createNewOnNotExistingFile() throws Exception {
    doWriteOnNotExistingFile(CREATE_NEW);
  }

  @Test
  public void appendOnExistingFile() throws Exception {
    String content = doWriteOnExistingFile(APPEND);
    assertThat(content, is(HELLO_WORLD + HELLO_WORLD));
  }

  @Test
  public void overwriteOnExistingFile() throws Exception {
    String content = doWriteOnExistingFile(OVERWRITE);
    assertThat(content, is(HELLO_WORLD));
  }

  @Test
  public void createNewOnExistingFile() throws Exception {
    expectedError.expectError(NAMESPACE, FILE_ALREADY_EXISTS, FileAlreadyExistsException.class,
                              "Use a different write mode or point to a path which doesn't exists");
    doWriteOnExistingFile(CREATE_NEW);
  }

  @Test
  public void appendOnNotExistingParentWithoutCreateFolder() throws Exception {
    expectedError.expectError(NAMESPACE, ILLEGAL_PATH, IllegalPathException.class,
                              "because path to it doesn't exist");
    doWriteOnNotExistingParentWithoutCreateFolder(APPEND);
  }

  @Test
  public void overwriteOnNotExistingParentWithoutCreateFolder() throws Exception {
    expectedError.expectError(NAMESPACE, ILLEGAL_PATH, IllegalPathException.class,
                              "because path to it doesn't exist");
    doWriteOnNotExistingParentWithoutCreateFolder(OVERWRITE);
  }

  @Test
  public void createNewOnNotExistingParentWithoutCreateFolder() throws Exception {
    expectedError.expectError(NAMESPACE, ILLEGAL_PATH, IllegalPathException.class,
                              "because path to it doesn't exist");
    doWriteOnNotExistingParentWithoutCreateFolder(CREATE_NEW);
  }

  @Test
  public void appendNotExistingFileWithCreatedParent() throws Exception {
    doWriteNotExistingFileWithCreatedParent(APPEND);
  }

  @Test
  public void overwriteNotExistingFileWithCreatedParent() throws Exception {
    doWriteNotExistingFileWithCreatedParent(OVERWRITE);
  }

  @Test
  public void createNewNotExistingFileWithCreatedParent() throws Exception {
    doWriteNotExistingFileWithCreatedParent(CREATE_NEW);
  }

  @Test
  public void writeOnReadFile() throws Exception {
    File file = temporaryFolder.newFile();
    FileUtils.writeStringToFile(file, "overwrite me!");

    Event event = flowRunner("readAndWrite").withVariable("path", file.getAbsolutePath()).run();

    assertThat(event.getMessageAsString(muleContext), equalTo(HELLO_WORLD));
  }

  @Test
  public void writeStaticContent() throws Exception {
    String path = String.format("%s/%s", temporaryFolder.newFolder().getPath(), TEST_FILENAME);
    doWrite("writeStaticContent", path, "", CREATE_NEW, false);

    String content = readPathAsString(path);
    assertThat(content, is(HELLO_WORLD));
  }

  @Test
  public void writeWithCustomEncoding() throws Exception {
    final String defaultEncoding = muleContext.getConfiguration().getDefaultEncoding();
    assertThat(defaultEncoding, is(notNullValue()));

    final String customEncoding =
        availableCharsets().keySet().stream().filter(encoding -> !encoding.equals(defaultEncoding)).findFirst().orElse(null);

    assertThat(customEncoding, is(notNullValue()));
    final String filename = "encoding.txt";

    doWrite("write", filename, HELLO_WORLD, CREATE_NEW, false, customEncoding);
    byte[] content = readFileToByteArray(new File(temporaryFolder.getRoot(), filename));

    assertThat(Arrays.equals(content, HELLO_WORLD.getBytes(customEncoding)), is(true));
  }

  private void doWriteNotExistingFileWithCreatedParent(FileWriteMode mode) throws Exception {
    File folder = temporaryFolder.newFolder();
    final String path = String.format("%s/a/b/%s", folder.getAbsolutePath(), TEST_FILENAME);

    doWrite(path, HELLO_WORLD, mode, true);

    String content = readPathAsString(path);
    assertThat(content, is(HELLO_WORLD));
  }


  private void doWriteOnNotExistingFile(FileWriteMode mode) throws Exception {
    String path = String.format("%s/%s", temporaryFolder.newFolder().getPath(), TEST_FILENAME);
    doWrite(path, HELLO_WORLD, mode, false);

    String content = readPathAsString(path);
    assertThat(content, is(HELLO_WORLD));
  }

  private void doWriteOnNotExistingParentWithoutCreateFolder(FileWriteMode mode) throws Exception {
    File folder = temporaryFolder.newFolder();
    final String path = String.format("%s/a/b/%s", folder.getAbsolutePath(), TEST_FILENAME);

    doWrite(path, HELLO_WORLD, mode, false);
  }

  private String doWriteOnExistingFile(FileWriteMode mode) throws Exception {
    File file = temporaryFolder.newFile();
    FileUtils.writeStringToFile(file, HELLO_WORLD);

    doWrite(file.getAbsolutePath(), HELLO_WORLD, mode, false);
    return readPathAsString(file.getAbsolutePath());
  }
}
