/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.UNKNOWN;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.EXCEPTION_MAPPINGS;

import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.exception.ExceptionMapper;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(ERROR_HANDLING)
@Story(EXCEPTION_MAPPINGS)
public class ErrorTypeLocatorTestCase extends AbstractMuleTestCase {

  private final ErrorTypeRepository repository = new DefaultErrorTypeRepository();

  @Test
  public void useDefaultErrorWhenNoMappingFound() {
    ErrorType mockedError = mock(ErrorType.class);
    ErrorType unknown = repository.getErrorType(UNKNOWN).get();
    ErrorTypeLocator locator = ErrorTypeLocator.builder(repository)
        .defaultExceptionMapper(ExceptionMapper.builder().addExceptionMapping(Exception.class, mockedError).build())
        .defaultError(unknown)
        .build();

    ErrorType expectedError = locator.lookupErrorType(Exception.class);
    assertThat(expectedError, is(sameInstance(mockedError)));

    ErrorType defaultError = locator.lookupErrorType(Throwable.class);
    assertThat(defaultError, is(sameInstance(unknown)));
  }
}
