/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.mule.extension.file.common.api.exceptions.FileError.CANNOT_REACH;
import static org.mule.extension.file.common.api.exceptions.FileError.CONNECTION_TIMEOUT;
import static org.mule.extension.file.common.api.exceptions.FileError.INVALID_CREDENTIALS;
import static org.mule.extension.file.common.api.exceptions.FileError.SERVICE_NOT_AVAILABLE;
import static org.mule.extension.file.common.api.exceptions.FileError.UNKNOWN_HOST;
import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mule.extension.FtpTestHarness;
import org.mule.extension.ftp.api.FTPConnectionException;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.util.TestConnectivityUtils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("FTP Extension")
@Stories("Negative Connectivity Testing")
public class FtpNegativeConnectivityTestCase extends FtpConnectorTestCase {

  private static final Matcher<Exception> ANYTHING =
      is(allOf(instanceOf(ConnectionException.class), hasCause(instanceOf(FTPConnectionException.class))));
  private final String name;
  private TestConnectivityUtils utils;

  @Rule
  public SystemProperty rule = TestConnectivityUtils.disableAutomaticTestConnectivity();

  public FtpNegativeConnectivityTestCase(String name, FtpTestHarness testHarness) {
    super(name, testHarness);
    this.name = name;
  }

  @Override
  protected String getConfigFile() {
    return name + "-negative-connectivity-test.xml";
  }

  @Before
  public void setUp() {
    utils = new TestConnectivityUtils(muleContext);
  }

  @Test
  public void configInvalidCredentials() {
    utils.assertFailedConnection(name + "ConfigInvalidCredentials", ANYTHING, is(errorType(INVALID_CREDENTIALS)));
  }

  @Test
  public void configConnectionTimeout() {
    utils.assertFailedConnection(name + "ConfigConnectionTimeout", ANYTHING, is(errorType(CONNECTION_TIMEOUT)));
  }

  @Test
  public void connectionRefused() {
    utils.assertFailedConnection(name + "ConfigConnectionRefused", ANYTHING, is(errorType(CANNOT_REACH)));
  }

  @Test
  public void configMissingCredentials() {
    utils.assertFailedConnection(name + "ConfigMissingCredentials", ANYTHING, is(errorType(INVALID_CREDENTIALS)));
  }

  @Test
  public void configUnknownHost() {
    utils.assertFailedConnection(name + "ConfigUnknownHost", ANYTHING, is(errorType(UNKNOWN_HOST)));
  }

  @Test
  public void ftpConfigServiceUnavailable() {
    // For some strange reason the usage of "assumeThat()" doesn't make the test being ignored and breaks it.
    // assumeThat(name, is("ftp"));
    if (name.equals("ftp")) {
      utils.assertSuccessConnection("ftpConfigFirstConnection");
      utils.assertFailedConnection("ftpConfigServiceUnavailable", ANYTHING, is(errorType(SERVICE_NOT_AVAILABLE)));
    }
  }
}
