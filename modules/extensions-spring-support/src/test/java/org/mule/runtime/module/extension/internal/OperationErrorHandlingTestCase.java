/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.mule.functional.junit4.rules.ExpectedError.none;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.core.exception.Errors.Identifiers.CONNECTIVITY_ERROR_IDENTIFIER;
import static org.mule.runtime.core.exception.Errors.Identifiers.UNKNOWN_ERROR_IDENTIFIER;
import static org.mule.test.heisenberg.extension.HeisenbergErrors.HEALTH;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.functional.junit4.rules.ExpectedError;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.exception.HeisenbergException;

import org.junit.Rule;
import org.junit.Test;

public class OperationErrorHandlingTestCase extends ExtensionFunctionalTestCase {

  private static final String HEISENBERG = "HEISENBERG";

  @Rule
  public ExpectedError expectedError = none();

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"heisenberg-operation-error-handling-config.xml"};
  }

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {HeisenbergExtension.class};
  }

  @Test
  public void heisenbergThrowsAHealthErrorFromHeisenbergException() throws Exception {
    expectedError.expectErrorType(HEISENBERG, HEALTH.getType()).expectCause(instanceOf(HeisenbergException.class));
    flowRunner("cureCancer").run();
  }

  @Test
  public void connectionExceptionThrowsAnConnectivityError() throws Exception {
    expectedError.expectErrorType(HEISENBERG, CONNECTIVITY_ERROR_IDENTIFIER).expectCause(instanceOf(ConnectionException.class));
    flowRunner("connectionFails").run();
  }

  @Test
  public void unrecognizedExceptionIsUnknown() throws Exception {
    expectedError
        .expectErrorType(CORE_PREFIX.toUpperCase(), UNKNOWN_ERROR_IDENTIFIER)
        .expectCause(instanceOf(HeisenbergException.class));
    flowRunner("unrecognizedException").run();
  }
}
