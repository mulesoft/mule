/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.retriever;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.mule.extension.email.api.exception.EmailError.CONNECTION_TIMEOUT;
import static org.mule.extension.email.api.exception.EmailError.INVALID_CREDENTIALS;
import static org.mule.extension.email.api.exception.EmailError.UNKNOWN_HOST;
import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mule.extension.email.EmailConnectorTestCase;
import org.mule.extension.email.api.exception.EmailConnectionException;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.util.TestConnectivityUtils;

public class IMAPNegativeConnectivityTestCase extends EmailConnectorTestCase {

  private TestConnectivityUtils connectivityUtils;

  @Rule
  public SystemProperty rule = TestConnectivityUtils.disableAutomaticTestConnectivity();

  @Override
  protected String getConfigFile() {
    return "retriever/imap-invalid-connections.xml";
  }

  @Override
  public String getProtocol() {
    return "imap";
  }

  @Before
  public void createUtils() {
    connectivityUtils = new TestConnectivityUtils(muleContext);
  }

  @Test
  public void configIncorrectCredentials() {
    Matcher<Exception> exceptionMatcher =
        is(allOf(instanceOf(ConnectionException.class), hasCause(instanceOf(EmailConnectionException.class))));
    connectivityUtils.assertFailedConnection("configIncorrectCredentials", exceptionMatcher, is(errorType(INVALID_CREDENTIALS)));
  }

  @Test
  public void configIncorrectCredentialsOnlyPassword() {
    Matcher<Exception> exceptionMatcher =
        is(allOf(instanceOf(ConnectionException.class), hasCause(instanceOf(EmailConnectionException.class))));
    connectivityUtils.assertFailedConnection("configIncorrectCredentialsOnlyPassword", exceptionMatcher,
                                             is(errorType(INVALID_CREDENTIALS)));
  }

  @Test
  public void configInvalidPort() {
    Matcher<Exception> exceptionMatcher =
        is(allOf(instanceOf(ConnectionException.class), hasCause(instanceOf(EmailConnectionException.class))));
    connectivityUtils.assertFailedConnection("configInvalidPort", exceptionMatcher, is(errorType(UNKNOWN_HOST)));
  }

  @Test
  public void configUnknownHost() {
    Matcher<Exception> exceptionMatcher =
        is(allOf(instanceOf(ConnectionException.class), hasCause(instanceOf(EmailConnectionException.class))));
    connectivityUtils.assertFailedConnection("configUnknownHost", exceptionMatcher, is(errorType(UNKNOWN_HOST)));
  }

  @Test
  public void configCannotReach() {
    Matcher<Exception> exceptionMatcher =
        is(allOf(instanceOf(ConnectionException.class), hasCause(instanceOf(EmailConnectionException.class))));
    connectivityUtils.assertFailedConnection("configCannotReach", exceptionMatcher, is(errorType(UNKNOWN_HOST)));
  }

  @Test
  public void configSocketTimeout() {
    Matcher<Exception> exceptionMatcher =
        is(allOf(instanceOf(ConnectionException.class), hasCause(instanceOf(EmailConnectionException.class))));
    connectivityUtils.assertFailedConnection("configSocketTimeout", exceptionMatcher, is(errorType(CONNECTION_TIMEOUT)));
  }
}
