/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.extension.FtpTestHarness.HELLO_WORLD;
import static org.mule.extension.file.common.api.exceptions.FileError.FILE_ALREADY_EXISTS;
import static org.mule.extension.file.common.api.exceptions.FileError.ILLEGAL_PATH;
import org.mule.extension.FtpTestHarness;
import org.mule.extension.file.common.api.exceptions.FileAlreadyExistsException;
import org.mule.extension.file.common.api.exceptions.IllegalPathException;

import java.nio.file.Paths;

import org.junit.Test;

public class FtpCopyTestCase extends FtpConnectorTestCase {

  private static final String SOURCE_FILE_NAME = "test.txt";
  private static final String SOURCE_DIRECTORY_NAME = "source";
  private static final String TARGET_DIRECTORY = "target";
  private static final String EXISTING_CONTENT = "I was here first!";
  private static final String RENAMED = "renamed.txt";

  protected String sourcePath;

  public FtpCopyTestCase(String name, FtpTestHarness testHarness) {
    super(name, testHarness);
  }

  @Override
  protected String getConfigFile() {
    return "ftp-copy-config.xml";
  }

  private String getPath(String... path) throws Exception {
    return Paths.get(testHarness.getWorkingDirectory(), path).toString();
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    testHarness.write(SOURCE_FILE_NAME, HELLO_WORLD);
    sourcePath = getPath(SOURCE_FILE_NAME);
  }

  @Test
  public void toExistingFolder() throws Exception {
    testHarness.makeDir(TARGET_DIRECTORY);
    final String path = getPath(TARGET_DIRECTORY);
    doExecute(path, false, false);

    assertCopy(format("%s/%s", path, SOURCE_FILE_NAME));
  }

  @Test
  public void absoluteSourcePath() throws Exception {
    final String absoluteSourcePath = String.format("%s/%s", testHarness.getWorkingDirectory(), SOURCE_FILE_NAME);
    testHarness.makeDir(TARGET_DIRECTORY);
    final String path = getPath(TARGET_DIRECTORY);
    doExecute(getFlowName(), absoluteSourcePath, path, false, false, null);

    assertCopy(format("%s/%s", path, SOURCE_FILE_NAME));
  }

  @Test
  public void nullTarget() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, ILLEGAL_PATH.getType(), IllegalPathException.class,
                                            "target path cannot be null nor blank");
    doExecute(null, false, false);
  }

  @Test
  public void copyToItselfWithoutOverwrite() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, FILE_ALREADY_EXISTS.getType(), FileAlreadyExistsException.class,
                                            "already exists");
    doExecute(getFlowName(), sourcePath, sourcePath, false, false, null);
  }

  @Test
  public void toNonExistingFolder() throws Exception {
    testHarness.makeDir(TARGET_DIRECTORY);
    String target = format("%s/%s", TARGET_DIRECTORY, "a/b/c");
    doExecute(target, false, true);

    assertCopy(format("%s/%s", target, SOURCE_FILE_NAME));
  }

  @Test
  public void copyReadFile() throws Exception {
    testHarness.makeDir(TARGET_DIRECTORY);
    final String path = getPath(TARGET_DIRECTORY);
    doExecute("readAndDo", path, false, false, null);

    assertCopy(format("%s/%s", path, SOURCE_FILE_NAME));
  }

  @Test
  public void toNonExistingFolderWithoutCreateParent() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, ILLEGAL_PATH.getType(), IllegalPathException.class,
                                            "doesn't exists");
    testHarness.makeDir(TARGET_DIRECTORY);
    String target = format("%s/%s", TARGET_DIRECTORY, "a/b/c");
    doExecute(target, false, false);
  }

  @Test
  public void overwriteInSameDirectory() throws Exception {
    final String existingFileName = "existing";
    testHarness.write(existingFileName, EXISTING_CONTENT);

    final String target = getPath(existingFileName);

    doExecute(target, true, false);
    assertCopy(target);
  }

  @Test
  public void overwriteInDifferentDirectory() throws Exception {
    final String existingDir = "existingDir";
    testHarness.makeDir(existingDir);

    final String existingPath = existingDir + "/existing.txt";
    testHarness.write(existingPath, EXISTING_CONTENT);

    doExecute(existingPath, true, false);
    assertCopy(existingPath);
  }

  @Test
  public void withoutOverwrite() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, FILE_ALREADY_EXISTS.getType(), FileAlreadyExistsException.class,
                                            "already exists");
    final String existingFileName = "existing";
    testHarness.write(existingFileName, EXISTING_CONTENT);

    doExecute(getPath(existingFileName), false, false);
  }

  @Test
  public void directoryToExistingDirectory() throws Exception {
    sourcePath = buildSourceDirectory();
    final String target = "target";
    testHarness.makeDir(target);
    doExecute(target, false, false);
    assertCopy(format("%s/source/%s", target, SOURCE_FILE_NAME));
  }

  @Test
  public void directoryToNotExistingDirectory() throws Exception {
    sourcePath = buildSourceDirectory();

    String target = "a/b/c";
    doExecute(target, false, true);

    assertCopy(format("%s/source/%s", target, SOURCE_FILE_NAME));
  }

  @Test
  public void directoryAndOverwrite() throws Exception {
    sourcePath = buildSourceDirectory();

    final String target = "target";
    testHarness.makeDir(target);
    testHarness.changeWorkingDirectory(target);
    testHarness.makeDir(SOURCE_DIRECTORY_NAME);
    testHarness.write(SOURCE_DIRECTORY_NAME, SOURCE_FILE_NAME, EXISTING_CONTENT);

    testHarness.changeWorkingDirectory("../");

    doExecute(target, true, false);
    assertCopy(format("%s/%s/%s", target, SOURCE_DIRECTORY_NAME, SOURCE_FILE_NAME));
  }

  @Test
  public void directoryWithoutOverwrite() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, FILE_ALREADY_EXISTS.getType(), FileAlreadyExistsException.class,
                                            "already exists");
    sourcePath = buildSourceDirectory();

    final String target = "target";
    testHarness.makeDir(target);
    testHarness.changeWorkingDirectory(target);
    testHarness.makeDir(SOURCE_DIRECTORY_NAME);
    testHarness.write(SOURCE_DIRECTORY_NAME, SOURCE_FILE_NAME, EXISTING_CONTENT);
    testHarness.changeWorkingDirectory("../");

    doExecute(format("%s/%s", target, SOURCE_DIRECTORY_NAME), false, false);

  }

  @Test
  public void copyAndRenameInSameDirectory() throws Exception {
    doExecute(testHarness.getWorkingDirectory(), true, false, RENAMED);
    assertCopy(RENAMED);
  }

  @Test
  public void copyAndRenameInSameDirectoryWithOverwrite() throws Exception {
    testHarness.write(testHarness.getWorkingDirectory(), RENAMED, EXISTING_CONTENT);

    doExecute(testHarness.getWorkingDirectory(), true, false, RENAMED);
    assertCopy(RENAMED);
  }

  @Test
  public void copyAndRenameInSameDirectoryWithoutOverwrite() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, FILE_ALREADY_EXISTS, FileAlreadyExistsException.class, "already exists");
    testHarness.write(testHarness.getWorkingDirectory(), RENAMED, EXISTING_CONTENT);

    doExecute(testHarness.getWorkingDirectory(), false, false, RENAMED);
  }

  @Test
  public void directoryToExistingDirectoryWithRename() throws Exception {
    sourcePath = buildSourceDirectory();
    final String target = "target";
    testHarness.makeDir(target);
    doExecute(target, false, false, "renamedSource");
    assertCopy(format("%s/renamedSource/%s", target, SOURCE_FILE_NAME));
  }

  private String buildSourceDirectory() throws Exception {
    testHarness.makeDir(SOURCE_DIRECTORY_NAME);
    testHarness.write(SOURCE_DIRECTORY_NAME, SOURCE_FILE_NAME, HELLO_WORLD);

    return getPath(SOURCE_DIRECTORY_NAME);
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
