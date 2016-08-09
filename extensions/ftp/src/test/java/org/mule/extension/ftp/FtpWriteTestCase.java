/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static java.nio.charset.Charset.availableCharsets;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.extension.FtpTestHarness.HELLO_WORLD;
import static org.mule.runtime.core.util.IOUtils.toByteArray;
import static org.mule.runtime.module.extension.file.api.FileWriteMode.APPEND;
import static org.mule.runtime.module.extension.file.api.FileWriteMode.CREATE_NEW;
import static org.mule.runtime.module.extension.file.api.FileWriteMode.OVERWRITE;
import org.mule.extension.FtpTestHarness;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.module.extension.file.api.FileWriteMode;

import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.Test;

public class FtpWriteTestCase extends FtpConnectorTestCase {

  private static final String TEMP_DIRECTORY = "files";

  public FtpWriteTestCase(String name, FtpTestHarness testHarness) {
    super(name, testHarness);
  }

  @Override
  protected String getConfigFile() {
    return "ftp-write-config.xml";
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
    testHarness.expectedException().expectCause(instanceOf(IllegalArgumentException.class));
    doWriteOnExistingFile(CREATE_NEW);
  }

  @Test
  public void appendOnNotExistingParentWithoutCreateFolder() throws Exception {
    testHarness.expectedException().expectCause(instanceOf(IllegalArgumentException.class));
    doWriteOnNotExistingParentWithoutCreateFolder(APPEND);
  }

  @Test
  public void overwriteOnNotExistingParentWithoutCreateFolder() throws Exception {
    testHarness.expectedException().expectCause(instanceOf(IllegalArgumentException.class));
    doWriteOnNotExistingParentWithoutCreateFolder(OVERWRITE);
  }

  @Test
  public void createNewOnNotExistingParentWithoutCreateFolder() throws Exception {
    testHarness.expectedException().expectCause(instanceOf(IllegalArgumentException.class));
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
    final String filePath = "file";

    testHarness.write(filePath, "overwrite me!");

    MuleEvent event = flowRunner("readAndWrite").withFlowVariable("path", filePath).run();

    assertThat(event.getMessageAsString(), equalTo(HELLO_WORLD));
  }

  @Test
  public void writeStaticContent() throws Exception {
    testHarness.makeDir(TEMP_DIRECTORY);
    String path = Paths.get(testHarness.getWorkingDirectory(), TEMP_DIRECTORY, "test.txt").toString();
    doWrite("writeStaticContent", path, "", CREATE_NEW, false);

    String content = getPayloadAsString(readPath(path));
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
    InputStream content =
        (InputStream) readPath(Paths.get(testHarness.getWorkingDirectory()).resolve(filename).toString()).getPayload();

    assertThat(Arrays.equals(toByteArray(content), HELLO_WORLD.getBytes(customEncoding)), is(true));
  }

  private void doWriteNotExistingFileWithCreatedParent(FileWriteMode mode) throws Exception {
    testHarness.makeDir(TEMP_DIRECTORY);
    String path = Paths.get(testHarness.getWorkingDirectory(), TEMP_DIRECTORY, "a/b/test.txt").toString();

    doWrite(path, HELLO_WORLD, mode, true);

    String content = getPayloadAsString(readPath(path));
    assertThat(content, is(HELLO_WORLD));
  }


  private void doWriteOnNotExistingFile(FileWriteMode mode) throws Exception {
    testHarness.makeDir(TEMP_DIRECTORY);
    String path = Paths.get(testHarness.getWorkingDirectory(), TEMP_DIRECTORY, "test.txt").toString();
    doWrite(path, HELLO_WORLD, mode, false);

    String content = getPayloadAsString(readPath(path));
    assertThat(content, is(HELLO_WORLD));
  }

  private void doWriteOnNotExistingParentWithoutCreateFolder(FileWriteMode mode) throws Exception {
    testHarness.makeDir(TEMP_DIRECTORY);
    String path = Paths.get(testHarness.getWorkingDirectory(), TEMP_DIRECTORY, "a/b/test.txt").toString();
    doWrite(path, HELLO_WORLD, mode, false);
  }

  private String doWriteOnExistingFile(FileWriteMode mode) throws Exception {
    final String filePath = "file";
    testHarness.write(filePath, HELLO_WORLD);

    doWrite(filePath, HELLO_WORLD, mode, false);
    return getPayloadAsString(readPath(filePath));
  }
}
