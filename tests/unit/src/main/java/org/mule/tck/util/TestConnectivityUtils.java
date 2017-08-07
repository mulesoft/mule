/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.runtime.api.connectivity.ConnectivityTestingService.CONNECTIVITY_TESTING_SERVICE_KEY;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.tck.junit4.rule.SystemProperty;

import org.hamcrest.Matcher;

/**
 * Utils to do connectivity testing over configurations on a Mule application
 *
 * @since 4.0
 */
public class TestConnectivityUtils {

  private static final Matcher NULL_VALUE = nullValue();
  private MuleContext muleContext;

  public TestConnectivityUtils(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  /**
   * Executes a test connectivity and excepts that it finish correctly
   * @param configName name of the Config to do a test connectivity
   */
  public void assertSuccessConnection(String configName) {
    assertConnection(configName, true, NULL_VALUE, NULL_VALUE);
  }

  /**
   * Executes as test connectivity over a the connection bind with the given {@code configName} and asserts a failed
   * connectivity test.
   *
   * @param configName name of the Config to do a test connectivity
   * @param exceptionMatcher matcher to assert characteristics of the failed test connectivity
   * @param errorTypeMatcher matcher to assert characteristics of the given {@link ErrorType}
   */
  public void assertFailedConnection(String configName, Matcher<Exception> exceptionMatcher,
                                     Matcher<ErrorType> errorTypeMatcher) {
    assertConnection(configName, false, exceptionMatcher, errorTypeMatcher);
  }

  /**
   * Injects a System property to disable automatic test connectivity when the Mule app starts
   */
  public static SystemProperty disableAutomaticTestConnectivity() {
    return new SystemProperty("doTestConnectivity", "false");
  }

  private void assertConnection(String configName, boolean isSuccess, Matcher<Exception> exceptionMatcher,
                                Matcher<ErrorType> codeMatcher) {
    ConnectivityTestingService testingService = muleContext.getRegistry().get(CONNECTIVITY_TESTING_SERVICE_KEY);
    ConnectionValidationResult validationResult = testingService.testConnection(builder().globalName(configName).build());
    assertThat(validationResult.isValid(), is(isSuccess));
    if (!isSuccess) {
      assertThat(validationResult.getException(), exceptionMatcher);
      assertThat(validationResult.getErrorType().orElse(null), codeMatcher);
    }
  }
}
