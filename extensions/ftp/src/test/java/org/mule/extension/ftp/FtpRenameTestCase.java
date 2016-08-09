/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.FtpTestHarness.HELLO_FILE_NAME;
import static org.mule.extension.FtpTestHarness.HELLO_PATH;
import static org.mule.extension.FtpTestHarness.HELLO_WORLD;
import org.mule.extension.FtpTestHarness;

import java.nio.file.Paths;

import org.junit.Test;

public class FtpRenameTestCase extends FtpConnectorTestCase {

  private static final String RENAME_TO = "renamed";

  public FtpRenameTestCase(String name, FtpTestHarness testHarness) {
    super(name, testHarness);
  }

  @Override
  protected String getConfigFile() {
    return "ftp-rename-config.xml";
  }

  @Test
  public void renameFile() throws Exception {
    testHarness.createHelloWorldFile();
    doRename(HELLO_PATH);
    assertRenamedFile();
  }

  @Test
  public void renameReadFile() throws Exception {
    testHarness.createHelloWorldFile();
    doRename("readAndRename", HELLO_PATH, RENAME_TO, false);
    assertRenamedFile();
  }

  @Test
  public void renameDirectory() throws Exception {
    testHarness.createHelloWorldFile();
    final String sourcePath = Paths.get(HELLO_PATH).getParent().toString();
    doRename(sourcePath);

    assertThat(testHarness.dirExists(sourcePath), is(false));
    assertThat(testHarness.dirExists(RENAME_TO), is(true));

    assertThat(readPathAsString(String.format("%s/%s", RENAME_TO, HELLO_FILE_NAME)), is(HELLO_WORLD));
  }

  @Test
  public void renameUnexisting() throws Exception {
    testHarness.expectedException().expectCause(instanceOf(IllegalArgumentException.class));
    doRename("not-there.txt");
  }

  @Test
  public void targetPathContainsParts() throws Exception {
    testHarness.createHelloWorldFile();
    final String sourcePath = Paths.get(HELLO_PATH).getParent().toString();
    testHarness.expectedException().expectCause(instanceOf(IllegalArgumentException.class));
    doRename("rename", sourcePath, "path/with/parts", true);
  }

  @Test
  public void targetAlreadyExistsWithoutOverwrite() throws Exception {
    final String sourceFile = "renameme.txt";
    testHarness.write(sourceFile, "rename me");
    testHarness.write(RENAME_TO, "I was here first");

    testHarness.expectedException().expectCause(instanceOf(IllegalArgumentException.class));
    doRename(sourceFile);
  }

  @Test
  public void targetAlreadyExistsWithOverwrite() throws Exception {
    testHarness.createHelloWorldFile();
    final String sourcePath = Paths.get(HELLO_PATH).getParent().resolve(RENAME_TO).toString();
    testHarness.write(sourcePath, "I was here first");

    doRename(HELLO_PATH, true);
    assertRenamedFile();
  }

  private void assertRenamedFile() throws Exception {
    final String targetPath =
        Paths.get(testHarness.getWorkingDirectory()).resolve(HELLO_PATH).getParent().resolve(RENAME_TO).toString();

    assertThat(testHarness.fileExists(targetPath), is((true)));
    assertThat(testHarness.fileExists(HELLO_PATH), is((false)));
    assertThat(readPathAsString(targetPath), is(HELLO_WORLD));
  }

  private void doRename(String source) throws Exception {
    doRename("rename", source, RENAME_TO, false);
  }

  private void doRename(String source, boolean overwrite) throws Exception {
    doRename("rename", source, RENAME_TO, overwrite);
  }

  private void doRename(String flow, String source, String to, boolean overwrite) throws Exception {
    flowRunner(flow).withFlowVariable("path", source).withFlowVariable("to", to).withFlowVariable("overwrite", overwrite).run();
  }
}
