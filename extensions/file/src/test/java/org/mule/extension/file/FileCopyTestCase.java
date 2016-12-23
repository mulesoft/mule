/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file;

import static java.lang.String.format;
import static org.apache.commons.io.FileUtils.write;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.extension.file.common.api.exceptions.FileErrors.FILE_ALREADY_EXISTS;
import static org.mule.extension.file.common.api.exceptions.FileErrors.ILLEGAL_PATH;
import org.mule.extension.file.common.api.exceptions.FileAlreadyExistsException;
import org.mule.extension.file.common.api.exceptions.IllegalPathException;
import org.mule.functional.junit4.FlowRunner;
import org.mule.runtime.core.exception.MessagingException;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class FileCopyTestCase extends FileConnectorTestCase {

  private static final String SOURCE_FILE_NAME = "test.txt";
  private static final String SOURCE_DIRECTORY_NAME = "source";
  private static final String EXISTING_CONTENT = "I was here first!";

  protected String sourcePath;

  @Override
  protected String getConfigFile() {
    return "file-copy-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    File sourceFile = temporaryFolder.newFile(SOURCE_FILE_NAME);
    write(sourceFile, HELLO_WORLD);
    sourcePath = sourceFile.getAbsolutePath();
  }

  @Test
  public void toExistingFolder() throws Exception {
    String target = temporaryFolder.newFolder().getAbsolutePath();
    getFlow(target, false, false).run();

    assertCopy(format("%s/%s", target, SOURCE_FILE_NAME));
  }

  @Test
  public void nullTarget() throws Exception {
    assertError(getFlow(null, false, false).runExpectingException(),
                ILLEGAL_PATH.getType(), IllegalPathException.class, "target path cannot be null nor blank");
  }

  @Test
  public void copyToItselfWithoutOverwrite() throws Exception {
    assertError(getFlow(getFlowName(), sourcePath, sourcePath, false, false).runExpectingException(),
                FILE_ALREADY_EXISTS.getType(), FileAlreadyExistsException.class, "already exists");
  }

  @Test
  public void copyReadFile() throws Exception {
    String target = temporaryFolder.newFolder().getAbsolutePath();
    getFlow("readAndDo", target, false, false).run();

    assertCopy(format("%s/%s", target, SOURCE_FILE_NAME));
  }

  @Test
  public void toNonExistingFolder() throws Exception {
    String target = format("%s/%s", temporaryFolder.newFolder().getAbsolutePath(), "a/b/c");
    getFlow(target, false, true).run();

    assertCopy(format("%s/%s", target, SOURCE_FILE_NAME));
  }

  @Test
  public void toNonExistingFolderWithoutCreateParent() throws Exception {
    String target = temporaryFolder.newFile().getAbsolutePath() + "a/b/c";
    assertError(getFlow(target, false, false).runExpectingException(),
                ILLEGAL_PATH.getType(), IllegalPathException.class, "destination path doesn't exists");
  }

  @Test
  public void overwriteInSameDirectory() throws Exception {
    File existingFile = temporaryFolder.newFile();
    write(existingFile, EXISTING_CONTENT);

    final String target = existingFile.getAbsolutePath();

    getFlow(target, true, false).run();
    assertCopy(target);
  }

  @Test
  public void overwriteInDifferentDirectory() throws Exception {
    String target = temporaryFolder.newFolder().getAbsolutePath();
    write(new File(target, SOURCE_FILE_NAME), HELLO_WORLD);
    getFlow(target, true, false).run();

    assertCopy(format("%s/%s", target, SOURCE_FILE_NAME));
  }

  @Test
  public void withoutOverwrite() throws Exception {
    File existingFile = temporaryFolder.newFile();
    write(existingFile, EXISTING_CONTENT);

    assertError(getFlow(existingFile.getAbsolutePath(), false, false).runExpectingException(),
                FILE_ALREADY_EXISTS.getType(), FileAlreadyExistsException.class, "already exists");
  }

  @Test
  public void directoryToExistingDirectory() throws Exception {
    File sourceFolder = buildSourceDirectory();

    sourcePath = sourceFolder.getAbsolutePath();

    File targetFolder = temporaryFolder.newFolder("target");
    getFlow(targetFolder.getAbsolutePath(), false, false).run();
    assertCopy(format("%s/source/%s", targetFolder.getAbsolutePath(), SOURCE_FILE_NAME));
  }

  @Test
  public void directoryToNotExistingDirectory() throws Exception {
    File sourceFolder = buildSourceDirectory();

    sourcePath = sourceFolder.getAbsolutePath();

    String target = "a/b/c";
    getFlow(target, false, true).run();

    assertCopy(format("%s/source/%s", target, SOURCE_FILE_NAME));
  }

  @Test
  public void directoryAndOverwrite() throws Exception {
    sourcePath = buildSourceDirectory().getAbsolutePath();

    File targetDirectory = temporaryFolder.newFolder("target");
    File existingDirectory = new File(targetDirectory, SOURCE_DIRECTORY_NAME);
    existingDirectory.mkdir();
    File existingFile = new File(existingDirectory, SOURCE_FILE_NAME);
    write(existingFile, EXISTING_CONTENT);

    getFlow(targetDirectory.getAbsolutePath(), true, false).run();
    assertCopy(format("%s/%s/%s", targetDirectory.getAbsolutePath(), SOURCE_DIRECTORY_NAME, SOURCE_FILE_NAME));
  }

  @Test
  public void directoryWithoutOverwrite() throws Exception {
    sourcePath = buildSourceDirectory().getAbsolutePath();

    File targetDirectory = temporaryFolder.newFolder("target");
    File existingDirectory = new File(targetDirectory, SOURCE_DIRECTORY_NAME);
    existingDirectory.mkdir();
    File existingFile = new File(existingDirectory, SOURCE_FILE_NAME);
    write(existingFile, EXISTING_CONTENT);

    MessagingException exception =
        getFlow(format("%s/%s", targetDirectory.getAbsolutePath(), SOURCE_DIRECTORY_NAME), false, false)
            .runExpectingException();

    assertError(exception, FILE_ALREADY_EXISTS.getType(), FileAlreadyExistsException.class, "already exists");
  }

  private File buildSourceDirectory() throws IOException {
    File sourceFolder = temporaryFolder.newFolder(SOURCE_DIRECTORY_NAME);
    File file = new File(sourceFolder, SOURCE_FILE_NAME);
    write(file, HELLO_WORLD);
    return sourceFolder;
  }

  private FlowRunner getFlow(String target, boolean overwrite, boolean createParentFolder) throws Exception {
    return getFlow(getFlowName(), target, overwrite, createParentFolder);
  }

  private FlowRunner getFlow(String flowName, String target, boolean overwrite, boolean createParentFolder) throws Exception {
    return getFlow(flowName, sourcePath, target, overwrite, createParentFolder);
  }

  private FlowRunner getFlow(String flowName, String source, String target, boolean overwrite, boolean createParentFolder)
      throws Exception {
    return flowRunner(flowName).withVariable(SOURCE_DIRECTORY_NAME, source).withVariable("target", target)
        .withVariable("overwrite", overwrite).withVariable("createParent", createParentFolder);

  }

  protected void assertCopy(String target) throws Exception {
    assertThat(readPathAsString(target), equalTo(HELLO_WORLD));
  }

  protected String getFlowName() {
    return "copy";
  }
}
