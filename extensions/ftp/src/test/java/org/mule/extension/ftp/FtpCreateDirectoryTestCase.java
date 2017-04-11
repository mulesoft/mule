/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static org.mule.extension.file.common.api.exceptions.FileError.FILE_ALREADY_EXISTS;
import static org.mule.test.allure.AllureConstants.FtpFeature.FTP_EXTENSION;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.extension.FtpTestHarness;
import org.mule.extension.file.common.api.exceptions.FileAlreadyExistsException;

import java.nio.file.Paths;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;

@Features(FTP_EXTENSION)
public class FtpCreateDirectoryTestCase extends CommonFtpConnectorTestCase {

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
    testHarness.expectedError().expectError(NAMESPACE, FILE_ALREADY_EXISTS.getType(), FileAlreadyExistsException.class,
                                            "already exists");
    final String directory = "washerefirst";
    testHarness.makeDir(directory);
    doCreateDirectory(directory);
  }

  @Test
  public void createDirectoryWithComplexPath() throws Exception {
    final String base = testHarness.getWorkingDirectory();
    doCreateDirectory(Paths.get(base).resolve(DIRECTORY).toAbsolutePath().toString());

    assertThat(testHarness.dirExists(DIRECTORY), is(true));
  }

  private void doCreateDirectory(String directory) throws Exception {
    flowRunner("createDirectory").withVariable("directory", directory).run();
  }
}
