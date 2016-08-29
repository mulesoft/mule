/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mule.extension.FtpTestHarness.HELLO_WORLD;
import org.mule.extension.FtpTestHarness;
import org.mule.runtime.core.exception.MessagingException;

import java.nio.file.Paths;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class FtpCopyTestCase extends FtpConnectorTestCase {

  private static final String SOURCE_FILE_NAME = "test.txt";
  private static final String SOURCE_DIRECTORY_NAME = "source";
  private static final String TARGET_DIRECTORY = "target";
  private static final String EXISTING_CONTENT = "I was here first!";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

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
    doExecute("readAndDo", path, false, false);

    assertCopy(format("%s/%s", path, SOURCE_FILE_NAME));
  }

  @Test
  public void toNonExistingFolderWithoutCreateParent() throws Exception {
    testHarness.makeDir(TARGET_DIRECTORY);
    String target = format("%s/%s", TARGET_DIRECTORY, "a/b/c");
    testHarness.expectedException().expectCause(instanceOf(IllegalArgumentException.class));
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
    final String existingFileName = "existing";
    testHarness.write(existingFileName, EXISTING_CONTENT);
    final String target = getPath(existingFileName);

    testHarness.expectedException().expectCause(instanceOf(IllegalArgumentException.class));
    doExecute(target, false, false);
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
    sourcePath = buildSourceDirectory();

    final String target = "target";
    testHarness.makeDir(target);
    testHarness.changeWorkingDirectory(target);
    testHarness.makeDir(SOURCE_DIRECTORY_NAME);
    testHarness.write(SOURCE_DIRECTORY_NAME, SOURCE_FILE_NAME, EXISTING_CONTENT);
    testHarness.changeWorkingDirectory("../");

    testHarness.expectedException().expectCause(instanceOf(IllegalArgumentException.class));
    doExecute(format("%s/%s", target, SOURCE_DIRECTORY_NAME), false, false);
  }

  private String buildSourceDirectory() throws Exception {
    testHarness.makeDir(SOURCE_DIRECTORY_NAME);
    testHarness.write(SOURCE_DIRECTORY_NAME, SOURCE_FILE_NAME, HELLO_WORLD);

    return getPath(SOURCE_DIRECTORY_NAME);
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

  @Override
  protected String readPathAsString(String path) throws Exception {
    return super.readPathAsString(path);
  }

  protected String getFlowName() {
    return "copy";
  }
}
