/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.email.retriever;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mule.extension.email.EmailConnectorTestCase;
import org.mule.extension.email.api.exception.EmailConnectionException;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.streaming.object.CursorIterator;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.util.TestConnectivityUtils;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.mule.extension.email.api.exception.EmailError.INVALID_CREDENTIALS;
import static org.mule.extension.email.util.EmailTestUtils.JUANI_EMAIL;
import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;

public class IMAPSpecialCharactersTestCase extends EmailConnectorTestCase {

  private TestConnectivityUtils connectivityUtils;

  @Rule
  public SystemProperty rule = TestConnectivityUtils.disableAutomaticTestConnectivity();

  private static final String SPECIAL_CHARACTER_PASSWORD = "*uawH*IDXlh2p%21xSPOx%23%25zLpL";

  @Rule
  public SystemProperty specialCharacterPassword = new SystemProperty("specialCharacterPassword", SPECIAL_CHARACTER_PASSWORD);

  public IMAPSpecialCharactersTestCase() {
    super(JUANI_EMAIL, SPECIAL_CHARACTER_PASSWORD);
  }

  @Override
  protected String getConfigFile() {
    return "retriever/imap-special-characters-password.xml";
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
  public void configSpecialCharacterCredentials() {
    /*
    Matcher<Exception> exceptionMatcher =
        CoreMatchers.is(allOf(instanceOf(ConnectionException.class), hasCause(instanceOf(EmailConnectionException.class))));
    connectivityUtils.assertFailedConnection("configSpecialCharacterCredentials", exceptionMatcher,
                                             CoreMatchers.is(errorType(INVALID_CREDENTIALS)));
    */
    connectivityUtils.assertSuccessConnection("configSpecialCharacterCredentials");
  }

}
