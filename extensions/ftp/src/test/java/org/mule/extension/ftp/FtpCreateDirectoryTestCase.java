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
import org.mule.extension.FtpTestHarness;

import java.nio.file.Paths;

import org.junit.Test;

public class FtpCreateDirectoryTestCase extends FtpConnectorTestCase {

  private static final String DIRECTORY = "validDirectory";

  public FtpCreateDirectoryTestCase(String name, FtpTestHarness testHarness) {
    super(name, testHarness);
  }

  @Override
  protected String getConfigFile() {
    return "ftp-create-directory-config.xml";
  }

  @Test
  public void createDirectory() throws Exception {
    doCreateDirectory(DIRECTORY);
    assertThat(testHarness.dirExists(DIRECTORY), is(true));
  }

  @Test
  public void createExistingDirectory() throws Exception {
    final String directory = "washerefirst";
    testHarness.makeDir(directory);
    testHarness.expectedException().expectCause(instanceOf(IllegalArgumentException.class));

    doCreateDirectory(directory);
  }

  @Test
  public void createDirectoryWithComplexPath() throws Exception {
    final String base = testHarness.getWorkingDirectory();
    doCreateDirectory(Paths.get(base).resolve(DIRECTORY).toAbsolutePath().toString());

    assertThat(testHarness.dirExists(DIRECTORY), is(true));
  }

  private void doCreateDirectory(String directory) throws Exception {
    flowRunner("createDirectory").withFlowVariable("directory", directory).run();
  }
}
