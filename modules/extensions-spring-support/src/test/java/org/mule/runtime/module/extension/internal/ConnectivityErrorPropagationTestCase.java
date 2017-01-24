/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.tck.util.TestConnectivityUtils.disableAutomaticTestConnectivity;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;
import static org.mule.test.heisenberg.extension.HeisenbergErrors.HEALTH;
import static org.mule.test.heisenberg.extension.HeisenbergErrors.OAUTH2;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.tck.util.TestConnectivityUtils;
import org.mule.test.heisenberg.extension.HeisenbergErrors;

public class ConnectivityErrorPropagationTestCase extends ExtensionFunctionalTestCase {

  public static final ModuleException OAUTH_MODULE_EXCEPTION = new ModuleException(null, OAUTH2);
  public static final ModuleException HEALTH_MODULE_EXCEPTION = new ModuleException(null, HEALTH);
  public static final CustomConnectionException DOMAIN_HEALTH_CONNECTION_EXCEPTION =
      new CustomConnectionException(HEALTH_MODULE_EXCEPTION);
  public static final CustomConnectionException DOMAIN_OAUTH_CONNECTION_EXCEPTION =
      new CustomConnectionException(OAUTH_MODULE_EXCEPTION);
  public static final ConnectionException CONNECTION_EXCEPTION = new ConnectionException("Some Error", HEALTH_MODULE_EXCEPTION);
  private TestConnectivityUtils utils;

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class[] {SomeExtension.class};
  }

  @Before
  public void setUp() {
    utils = new TestConnectivityUtils(muleContext);
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

  @Extension(name = "SomeExtension")
  @ConnectionProviders(ExtConnProvider.class)
  @ErrorTypes(HeisenbergErrors.class)
  @Operations(SomeOps.class)
  public static class SomeExtension {

  }

  public static class ExtConnProvider implements CachedConnectionProvider<String> {

    @Parameter
    @Optional(defaultValue = "false")
    public boolean fail;

    @Parameter
    @Optional(defaultValue = "false")
    public boolean domainException;

    @Override
    public String connect() throws ConnectionException {
      if (fail) {
        throw domainException
            ? DOMAIN_HEALTH_CONNECTION_EXCEPTION
            : CONNECTION_EXCEPTION;
      }
      return "";
    }

    @Override
    public void disconnect(String connection) {

    }

    @Override
    public ConnectionValidationResult validate(String connection) {
      return domainException
          ? failure("This is a failure", DOMAIN_OAUTH_CONNECTION_EXCEPTION)
          : failure("This is a failure", OAUTH_MODULE_EXCEPTION);
    }


  }

  public static class CustomConnectionException extends ConnectionException {

    public CustomConnectionException(ModuleException e) {
      super("This is the message", e);
    }
  }

  public static class SomeOps {

    public void someOp(@Connection String conn, @UseConfig SomeExtension ext) {

    }
  }
}
