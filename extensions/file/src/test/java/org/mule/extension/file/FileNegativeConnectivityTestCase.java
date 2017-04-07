/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file;

import static org.mule.extension.file.common.api.exceptions.FileError.FILE_DOESNT_EXIST;
import static org.mule.extension.file.common.api.exceptions.FileError.FILE_IS_NOT_DIRECTORY;
import static org.mule.extension.file.common.api.exceptions.FileError.ILLEGAL_PATH;
import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;
import static org.mule.test.allure.AllureConstants.FileFeature.FILE_EXTENSION;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;

import org.mule.extension.file.api.exception.FileConnectionException;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.util.TestConnectivityUtils;

import java.io.IOException;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(FILE_EXTENSION)
@Stories("Negative Connectivity Testing")
public class FileNegativeConnectivityTestCase extends FileConnectorTestCase {

  private static final Matcher<Exception> CONNECTION_EXCEPTION_MATCHER =
      is(allOf(instanceOf(ConnectionException.class), hasCause(instanceOf(FileConnectionException.class))));
  private TestConnectivityUtils utils;

  @Rule
  public SystemProperty rule = TestConnectivityUtils.disableAutomaticTestConnectivity();

  @Override
  protected String getConfigFile() {
    return "file-negative-connectivity-test-config.xml";
  }

  @Before
  public void createUtils() {
    utils = new TestConnectivityUtils(muleContext);
  }

  @Test
  public void configFileDoesntExist() {
    utils.assertFailedConnection("configFileDoesntExist", CONNECTION_EXCEPTION_MATCHER, is(errorType(FILE_DOESNT_EXIST)));
  }

  @Test
  public void configFileIsNotDirectory() throws IOException {
    temporaryFolder.newFile("file.zip");
    utils.assertFailedConnection("configFileIsNotDirectory", CONNECTION_EXCEPTION_MATCHER, is(errorType(FILE_IS_NOT_DIRECTORY)));
  }

  @Test
  public void configIllegalPath() throws IOException {
    System.clearProperty("user.home");
    utils.assertFailedConnection("configIllegalPath", CONNECTION_EXCEPTION_MATCHER, is(errorType(ILLEGAL_PATH)));
  }

}
