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
import static org.mule.extension.file.common.api.exceptions.FileError.FILE_ALREADY_EXISTS;
import static org.mule.extension.file.common.api.exceptions.FileError.ILLEGAL_PATH;
import org.mule.extension.file.common.api.exceptions.FileAlreadyExistsException;
import org.mule.extension.file.common.api.exceptions.IllegalPathException;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class FileCopyTestCase extends FileConnectorTestCase {

  private static final String SOURCE_FILE_NAME = "test.txt";
  private static final String SOURCE_DIRECTORY_NAME = "source";
  private static final String EXISTING_CONTENT = "I was here first!";
  private static final String RENAMED = "renamed.txt";


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
    doExecute(target, false, false);

    assertCopy(format("%s/%s", target, SOURCE_FILE_NAME));
  }

  @Test
  public void nullTarget() throws Exception {
    expectedError.expectError(NAMESPACE, ILLEGAL_PATH, IllegalPathException.class,
                              "target path cannot be null nor blank");
    doExecute(null, false, false);
  }

  @Test
  public void copyToItselfWithoutOverwrite() throws Exception {
    expectedError.expectError(NAMESPACE, FILE_ALREADY_EXISTS, FileAlreadyExistsException.class, "already exists");
    doExecute(getFlowName(), sourcePath, sourcePath, false, false, null);
  }

  @Test
  public void copyReadFile() throws Exception {
    String target = temporaryFolder.newFolder().getAbsolutePath();
    doExecute("readAndDo", target, false, false, null);

    assertCopy(format("%s/%s", target, SOURCE_FILE_NAME));
  }

  @Test
  public void toNonExistingFolder() throws Exception {
    String target = format("%s/%s", temporaryFolder.newFolder().getAbsolutePath(), "a/b/c");
    doExecute(target, false, true);

    assertCopy(format("%s/%s", target, SOURCE_FILE_NAME));
  }

  @Test
  public void toNonExistingFolderWithoutCreateParent() throws Exception {
    expectedError.expectError(NAMESPACE, ILLEGAL_PATH, IllegalPathException.class, "destination path doesn't exists");
    String target = temporaryFolder.newFile().getAbsolutePath() + "a/b/c";
    doExecute(target, false, false);
  }

  @Test
  public void overwriteInSameDirectory() throws Exception {
    File existingFile = temporaryFolder.newFile();
    write(existingFile, EXISTING_CONTENT);

    final String target = existingFile.getAbsolutePath();

    doExecute(target, true, false);
    assertCopy(target);
  }

  @Test
  public void overwriteInDifferentDirectory() throws Exception {
    String target = temporaryFolder.newFolder().getAbsolutePath();
    write(new File(target, SOURCE_FILE_NAME), HELLO_WORLD);
    doExecute(target, true, false);

    assertCopy(format("%s/%s", target, SOURCE_FILE_NAME));
  }

  @Test
  public void withoutOverwrite() throws Exception {
    expectedError.expectError(NAMESPACE, FILE_ALREADY_EXISTS, FileAlreadyExistsException.class, "already exists");
    File existingFile = temporaryFolder.newFile();
    write(existingFile, EXISTING_CONTENT);

    doExecute(existingFile.getAbsolutePath(), false, false);
  }

  @Test
  public void directoryToExistingDirectory() throws Exception {
    File sourceFolder = buildSourceDirectory();

    sourcePath = sourceFolder.getAbsolutePath();

    File targetFolder = temporaryFolder.newFolder("target");
    doExecute(targetFolder.getAbsolutePath(), false, false);
    assertCopy(format("%s/source/%s", targetFolder.getAbsolutePath(), SOURCE_FILE_NAME));
  }

  @Test
  public void directoryToNotExistingDirectory() throws Exception {
    File sourceFolder = buildSourceDirectory();

    sourcePath = sourceFolder.getAbsolutePath();

    String target = "a/b/c";
    doExecute(target, false, true);

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

    doExecute(targetDirectory.getAbsolutePath(), true, false);
    assertCopy(format("%s/%s/%s", targetDirectory.getAbsolutePath(), SOURCE_DIRECTORY_NAME, SOURCE_FILE_NAME));
  }

  @Test
  public void directoryWithoutOverwrite() throws Exception {
    expectedError.expectError(NAMESPACE, FILE_ALREADY_EXISTS, FileAlreadyExistsException.class, "already exists");
    sourcePath = buildSourceDirectory().getAbsolutePath();

    File targetDirectory = temporaryFolder.newFolder("target");
    File existingDirectory = new File(targetDirectory, SOURCE_DIRECTORY_NAME);
    existingDirectory.mkdir();
    File existingFile = new File(existingDirectory, SOURCE_FILE_NAME);
    write(existingFile, EXISTING_CONTENT);

    doExecute(format("%s/%s", targetDirectory.getAbsolutePath(), SOURCE_DIRECTORY_NAME), false, false);
  }

  @Test
  public void copyAndRenameInSameDirectory() throws Exception {
    doExecute(temporaryFolder.getRoot().getAbsolutePath(), true, false, RENAMED);
    assertCopy(RENAMED);
  }

  @Test
  public void copyAndRenameInSameDirectoryWithOverwrite() throws Exception {
    File existingFile = temporaryFolder.newFile(RENAMED);
    write(existingFile, EXISTING_CONTENT);

    doExecute(existingFile.getParent(), true, false, RENAMED);
    assertCopy(RENAMED);
  }

  @Test
  public void copyAndRenameInSameDirectoryWithoutOverwrite() throws Exception {
    expectedError.expectError(NAMESPACE, FILE_ALREADY_EXISTS, FileAlreadyExistsException.class, "already exists");
    File existingFile = temporaryFolder.newFile(RENAMED);
    write(existingFile, EXISTING_CONTENT);
    doExecute(existingFile.getParent(), false, false, RENAMED);
  }

  @Test
  public void directoryToExistingDirectoryWithRename() throws Exception {
    File sourceFolder = buildSourceDirectory();

    sourcePath = sourceFolder.getAbsolutePath();

    File targetFolder = temporaryFolder.newFolder("target");
    doExecute(targetFolder.getAbsolutePath(), false, false, "renamedSource");
    assertCopy(format("%s/renamedSource/%s", targetFolder.getAbsolutePath(), SOURCE_FILE_NAME));
  }

  private File buildSourceDirectory() throws IOException {
    File sourceFolder = temporaryFolder.newFolder(SOURCE_DIRECTORY_NAME);
    File file = new File(sourceFolder, SOURCE_FILE_NAME);
    write(file, HELLO_WORLD);
    return sourceFolder;
  }

  void doExecute(String target, boolean overwrite, boolean createParentFolder) throws Exception {
    doExecute(getFlowName(), target, overwrite, createParentFolder, null);
  }

  void doExecute(String target, boolean overwrite, boolean createParentFolder, String renameTo) throws Exception {
    doExecute(getFlowName(), target, overwrite, createParentFolder, renameTo);
  }

  void doExecute(String flowName, String target, boolean overwrite, boolean createParentFolder, String renameTo)
      throws Exception {
    doExecute(flowName, sourcePath, target, overwrite, createParentFolder, renameTo);
  }

  void doExecute(String flowName, String source, String target, boolean overwrite, boolean createParentFolder,
                 String renameTo)
      throws Exception {
    flowRunner(flowName).withVariable(SOURCE_DIRECTORY_NAME, source).withVariable("target", target)
        .withVariable("overwrite", overwrite).withVariable("createParent", createParentFolder).withVariable("renameTo", renameTo)
        .run();

  }

  protected void assertCopy(String target) throws Exception {
    assertThat(readPathAsString(target), equalTo(HELLO_WORLD));
  }

  protected String getFlowName() {
    return "copy";
  }
}
