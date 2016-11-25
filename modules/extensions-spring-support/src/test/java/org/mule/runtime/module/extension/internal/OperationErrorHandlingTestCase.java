/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.core.exception.Errors.Identifiers.CONNECTIVITY_ERROR_IDENTIFIER;
import static org.mule.runtime.core.exception.Errors.Identifiers.UNKNOWN_ERROR_IDENTIFIER;
import static org.mule.test.heisenberg.extension.HeisenbergErrors.HEALTH;

import org.junit.Test;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.exception.HeisenbergException;

import java.util.Optional;

public class OperationErrorHandlingTestCase extends ExtensionFunctionalTestCase {

  private static final String HEISENBERG = "HEISENBERG";

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
    assertErrorType("cureCancer", HeisenbergException.class, HEALTH.getType(), HEISENBERG);
  }

  @Test
  public void connectionExceptionThrowsAnConnectivityError() throws Exception {
    assertErrorType("connectionFails", ConnectionException.class, CONNECTIVITY_ERROR_IDENTIFIER, HEISENBERG);
  }

  @Test
  public void unrecognizedExceptionIsUnknown() throws Exception {
    assertErrorType("unrecognizedException", HeisenbergException.class, UNKNOWN_ERROR_IDENTIFIER, HEISENBERG);
  }

  public void assertErrorType(String flowName, Class<? extends Throwable> throwable, String identifier, String namespace)
      throws Exception {
    MessagingException exception = flowRunner(flowName).runExpectingException();
    Optional<Error> errorOptional = exception.getEvent().getError();
    assertThat(errorOptional.isPresent(), is(true));
    Error unknownError = errorOptional.get();
    assertThat(unknownError.getCause(), is(instanceOf(throwable)));
    assertThat(unknownError.getErrorType().getIdentifier(), is(identifier));
    assertThat(unknownError.getErrorType().getNamespace(), is(namespace));
  }
}
