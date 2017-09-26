/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;
import static org.mule.tck.util.TestConnectivityUtils.disableAutomaticTestConnectivity;
import static org.mule.test.heisenberg.extension.HeisenbergErrors.HEALTH;
import static org.mule.test.heisenberg.extension.HeisenbergErrors.OAUTH2;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.util.TestConnectivityUtils;
import org.mule.test.some.extension.CustomConnectionException;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class ConnectivityErrorPropagationTestCase extends AbstractExtensionFunctionalTestCase {

  private TestConnectivityUtils utils;

  @Rule
  public SystemProperty rule = TestConnectivityUtils.disableAutomaticTestConnectivity();

  @Before
  public void setUp() {
    utils = new TestConnectivityUtils(registry);
  }

  @BeforeClass
  public static void disableConnectivityTesting() {
    disableAutomaticTestConnectivity();
  }

  @Override
  protected String getConfigFile() {
    return "connectivity-error-propagation-config.xml";
  }

  @Test
  public void failAtConnectWithConnectionException() {
    Matcher<Exception> exceptionMatcher =
        is(allOf(instanceOf(ConnectionException.class), hasCause(instanceOf(ModuleException.class))));
    utils.assertFailedConnection("failAtConnectWithConnectionException", exceptionMatcher, is(errorType(HEALTH)));
  }

  @Test
  public void failAtConnectWithDomainException() {
    Matcher<Exception> exceptionMatcher =
        is(allOf(instanceOf(ConnectionException.class), hasCause(instanceOf(CustomConnectionException.class))));
    utils.assertFailedConnection("failAtConnectWithDomainException", exceptionMatcher, is(errorType(HEALTH)));
  }

  @Test
  public void failAtValidateWithModuleException() {
    Matcher<Exception> exceptionMatcher = is(instanceOf(ModuleException.class));
    utils.assertFailedConnection("failAtValidateWithModuleException", exceptionMatcher, is(errorType(OAUTH2)));
  }

  @Test
  public void failAtValidateWithDomainException() {
    Matcher<Exception> exceptionMatcher = is(instanceOf(CustomConnectionException.class));
    utils.assertFailedConnection("failAtValidateWithDomainException", exceptionMatcher, is(errorType(OAUTH2)));
  }

}
