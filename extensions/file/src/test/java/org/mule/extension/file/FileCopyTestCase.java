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
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.exception.MessagingException;

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class FileCopyTestCase extends FileConnectorTestCase {

  private static final String SOURCE_FILE_NAME = "test.txt";
  private static final String SOURCE_DIRECTORY_NAME = "source";
  private static final String EXISTING_CONTENT = "I was here first!";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

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
    expectedException.expect(MessagingException.class);
    expectedException.expectCause(instanceOf(IllegalArgumentException.class));

    doExecute(null, false, false);
  }

  @Test
  public void copyToItselfWithoutOverwrite() throws Exception {
    expectedException.expect(MessagingException.class);
    expectedException.expectCause(instanceOf(IllegalArgumentException.class));

    doExecute(getFlowName(), sourcePath, sourcePath, false, false);
  }

  @Test
  public void copyReadFile() throws Exception {
    String target = temporaryFolder.newFolder().getAbsolutePath();
    doExecute("readAndDo", target, false, false);

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
    String target = temporaryFolder.newFile().getAbsolutePath() + "a/b/c";
    expectedException.expectCause(instanceOf(IllegalArgumentException.class));
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
    File existingFile = temporaryFolder.newFile();
    write(existingFile, EXISTING_CONTENT);

    expectedException.expectCause(instanceOf(IllegalArgumentException.class));
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
    sourcePath = buildSourceDirectory().getAbsolutePath();

    File targetDirectory = temporaryFolder.newFolder("target");
    File existingDirectory = new File(targetDirectory, SOURCE_DIRECTORY_NAME);
    existingDirectory.mkdir();
    File existingFile = new File(existingDirectory, SOURCE_FILE_NAME);
    write(existingFile, EXISTING_CONTENT);

    expectedException.expectCause(instanceOf(IllegalArgumentException.class));
    doExecute(format("%s/%s", targetDirectory.getAbsolutePath(), SOURCE_DIRECTORY_NAME), false, false);
  }

  private File buildSourceDirectory() throws IOException {
    File sourceFolder = temporaryFolder.newFolder(SOURCE_DIRECTORY_NAME);
    File file = new File(sourceFolder, SOURCE_FILE_NAME);
    write(file, HELLO_WORLD);
    return sourceFolder;
  }

  private void doExecute(String target, boolean overwrite, boolean createParentFolder) throws Exception {
    doExecute(getFlowName(), target, overwrite, createParentFolder);
  }

  private void doExecute(String flowName, String target, boolean overwrite, boolean createParentFolder) throws Exception {
    doExecute(flowName, sourcePath, target, overwrite, createParentFolder);
  }

  private void doExecute(String flowName, String source, String target, boolean overwrite, boolean createParentFolder)
      throws Exception {
    flowRunner(flowName).withFlowVariable(SOURCE_DIRECTORY_NAME, source).withFlowVariable("target", target)
        .withFlowVariable("overwrite", overwrite).withFlowVariable("createParent", createParentFolder).run();

  }

  protected void assertCopy(String target) throws Exception {
    assertThat(readPathAsString(target), equalTo(HELLO_WORLD));
  }

  protected String getFlowName() {
    return "copy";
  }
}
