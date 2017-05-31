/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.mule.functional.junit4.rules.ExpectedError.none;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.CRITICAL;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.FATAL;
import static org.mule.runtime.core.exception.Errors.Identifiers.CONNECTIVITY_ERROR_IDENTIFIER;
import static org.mule.runtime.core.exception.Errors.Identifiers.UNKNOWN_ERROR_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.test.heisenberg.extension.HeisenbergErrors.HEALTH;

import org.mule.functional.junit4.rules.ExpectedError;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.core.exception.Errors;
import org.mule.runtime.core.exception.MuleFatalException;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.test.heisenberg.extension.exception.HeisenbergException;

import org.junit.Rule;
import org.junit.Test;

public class OperationErrorHandlingTestCase extends AbstractExtensionFunctionalTestCase {

  private static final String HEISENBERG = "HEISENBERG";

  @Rule
  public ExpectedError expectedError = none();

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"heisenberg-operation-error-handling-config.xml"};
  }

  @Test
  public void heisenbergThrowsAHealthErrorFromHeisenbergException() throws Exception {
    expectedError.expectErrorType(HEISENBERG, HEALTH.getType()).expectCause(instanceOf(HeisenbergException.class));
    flowRunner("cureCancer").run();
  }

  @Test
  public void heisenbergThrowsMessagingExceptionWithEventAndFailingProcessorPopulated() throws Exception {
    Processor operation = ((Pipeline) getFlowConstruct("cureCancer")).getMessageProcessors().get(0);
    // Use good old try/catch because ExpectedError and ExpectedException rules don't like each other and it doesn't make sense to
    // put this test method elsewhere.
    try {
      flowRunner("cureCancer").run();
    } catch (MessagingException messagingException) {
      assertThat(messagingException.getFailingMessageProcessor(), is(operation));
      assertThat(messagingException.getEvent(), notNullValue());
    }
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

  @Test
  public void errorIsPropagatedCorrectly() throws Exception {
    expectedError
        .expectErrorType(FATAL.getNamespace().toUpperCase(), FATAL.getName())
        .expectCause(instanceOf(MuleFatalException.class));

    try {
      flowRunner("throwError").run();
      fail("Should've thrown an exception");

    } catch (Throwable t) {
      Throwable problem = t.getCause();
      assertThat(problem, instanceOf(MuleFatalException.class));
      assertThat(problem.getCause(), instanceOf(LinkageError.class));
      throw t;
    }
  }
}
