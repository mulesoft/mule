/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.mule.extension.validation.internal.ImmutableValidationResult.error;
import org.mule.extension.validation.api.ExceptionFactory;
import org.mule.extension.validation.api.ValidationException;
import org.mule.extension.validation.api.ValidationResult;
import org.mule.runtime.core.api.MuleEvent;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ValidationExceptionTestCase extends ValidationTestCase {

  private static final String MESSAGE_FAILED = "failed";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return "validation-exception.xml";
  }

  @Test
  public void byRefExceptionFactory() throws Exception {
    assertCustomExceptionFactory("byRefExceptionFactoryFlow");
  }

  @Test
  public void byClassExceptionFactory() throws Exception {
    assertCustomExceptionFactory("byClassExceptionFactoryFlow");
  }

  @Test
  public void globalExceptionFactory() throws Exception {
    assertCustomExceptionFactory("globalExceptionFactoryFlow");
  }

  @Test
  public void customMessage() throws Exception {
    expectedException.expectMessage("Hello World!");
    flowRunner("customMessage").run();
  }

  @Test
  public void customExceptionType() throws Exception {
    expectedException.expectCause(instanceOf(IllegalArgumentException.class));
    flowRunner("customExceptionType").run();
  }

  private void assertCustomExceptionFactory(String flowName) throws Exception {
    expectedException.expect(instanceOf(ValidationException.class));
    expectedException.expectMessage(MESSAGE_FAILED);
    flowRunner(flowName).run();
  }

  public static class TestExceptionFactory implements ExceptionFactory {

    @Override
    public <T extends Exception> T createException(ValidationResult result, Class<T> exceptionClass, MuleEvent event) {
      return (T) new ValidationException(error(MESSAGE_FAILED), event);
    }

    @Override
    public Exception createException(ValidationResult result, String exceptionClassName, MuleEvent event) {
      return new ValidationException(error(MESSAGE_FAILED), event);
    }
  }
}
