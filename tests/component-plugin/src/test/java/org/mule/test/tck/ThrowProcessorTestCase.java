/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.tck;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.EXPRESSION;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SECURITY;

import org.mule.functional.api.component.ThrowProcessor;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.api.exception.TypedException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.IOException;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ThrowProcessorTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ThrowProcessor throwProcessor = new ThrowProcessor();

  @Before
  public void setUp() throws MuleException {
    muleContext.getInjector().inject(throwProcessor);
  }

  @Test
  public void throwsExceptionIfNoError() throws MuleException {
    throwProcessor.setException(TestException.class);

    expectedException.expect(TestException.class);
    expectedException.expectCause(instanceOf(IOException.class));
    expectedException.expect(new TypedExceptionErrorMatcher(EXPRESSION));
    throwProcessor.process(mock(CoreEvent.class));
  }

  @Test
  public void throwsTypedExceptionIfError() throws MuleException {
    throwProcessor.setError(SECURITY.toString());
    throwProcessor.setException(IllegalArgumentException.class);

    expectedException.expect(TypedException.class);
    expectedException.expectCause(instanceOf(IllegalArgumentException.class));
    expectedException.expect(new TypedExceptionErrorMatcher(SECURITY));
    throwProcessor.process(mock(CoreEvent.class));
  }

  public static class TestException extends TypedException {

    public TestException() {
      super(new IOException(), muleContext.getErrorTypeRepository().lookupErrorType(EXPRESSION).get());
    }

  }

  private class TypedExceptionErrorMatcher extends TypeSafeMatcher<TypedException> {

    private ComponentIdentifier errorIdentifier;

    public TypedExceptionErrorMatcher(ComponentIdentifier errorIdentifier) {
      this.errorIdentifier = errorIdentifier;
    }

    @Override
    protected boolean matchesSafely(TypedException item) {
      ErrorType errorType = item.getErrorType();
      return errorIdentifier.getNamespace().equals(errorType.getNamespace()) && errorIdentifier.getName().equals(
                                                                                                                 errorType
                                                                                                                     .getIdentifier());
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("a TypedExeption with error ");
      description.appendValue(errorIdentifier);
    }

    @Override
    protected void describeMismatchSafely(TypedException exception, Description mismatchDescription) {
      mismatchDescription.appendText("is not a TypedException with error ");
      mismatchDescription.appendValue(exception.getErrorType());
    }
  }

}
