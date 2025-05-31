/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.connectivity;

import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;
import static org.mule.tck.util.TestConnectivityUtils.disableAutomaticTestConnectivity;
import static org.mule.test.heisenberg.extension.HeisenbergErrors.HEALTH;
import static org.mule.test.heisenberg.extension.HeisenbergErrors.OAUTH2;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.sdk.api.exception.ModuleException;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.util.TestConnectivityUtils;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.some.extension.CustomConnectionException;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class ConnectivityErrorPropagationTestCase extends AbstractExtensionFunctionalTestCase {

  private TestConnectivityUtils utils;

  @Rule
  public SystemProperty rule = disableAutomaticTestConnectivity();

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
    return "connectivity/connectivity-error-propagation-config.xml";
  }

  @Override
  public boolean addToolingObjectsToRegistry() {
    return true;
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
