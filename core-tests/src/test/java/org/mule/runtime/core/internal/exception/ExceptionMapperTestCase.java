/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.EXCEPTION_MAPPINGS;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.exception.ExceptionMapper;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(ERROR_HANDLING)
@Story(EXCEPTION_MAPPINGS)
public class ExceptionMapperTestCase extends AbstractMuleTestCase {

  private ErrorType runtimeExceptionErrorType = mock(ErrorType.class);
  private ErrorType illegalArgumentExceptionErrorType = mock(ErrorType.class);
  private ErrorType numberFormatExceptionErrorType = mock(ErrorType.class);

  private ErrorType classCastExceptionErrorType = mock(ErrorType.class);
  private ErrorType arrayStoreExceptionErrorType = mock(ErrorType.class);
  private ErrorType arrayStoreChildExceptionErrorType = mock(ErrorType.class);

  @Test
  public void sameHierarchyMapping() {
    ExceptionMapper exceptionMapper = ExceptionMapper.builder()
        .addExceptionMapping(RuntimeException.class, runtimeExceptionErrorType)
        .addExceptionMapping(NumberFormatException.class, numberFormatExceptionErrorType)
        .addExceptionMapping(IllegalArgumentException.class, illegalArgumentExceptionErrorType).build();

    assertThat(exceptionMapper.resolveErrorType(IllegalArgumentException.class).get(), is(illegalArgumentExceptionErrorType));
    assertThat(exceptionMapper.resolveErrorType(NumberFormatException.class).get(), is(numberFormatExceptionErrorType));
    assertThat(exceptionMapper.resolveErrorType(RuntimeException.class).get(), is(runtimeExceptionErrorType));
    assertThat(exceptionMapper.resolveErrorType(Exception.class).isPresent(), is(false));
  }

  @Test
  public void differentHierarchyMapping() {
    ExceptionMapper exceptionMapper = ExceptionMapper.builder()
        .addExceptionMapping(NumberFormatException.class, numberFormatExceptionErrorType)
        .addExceptionMapping(ClassCastException.class, classCastExceptionErrorType)
        .addExceptionMapping(ArrayStoreException.class, arrayStoreExceptionErrorType).build();

    assertThat(exceptionMapper.resolveErrorType(NumberFormatException.class).get(), is(numberFormatExceptionErrorType));
    assertThat(exceptionMapper.resolveErrorType(ArrayStoreException.class).get(), is(arrayStoreExceptionErrorType));
    assertThat(exceptionMapper.resolveErrorType(ClassCastException.class).get(), is(classCastExceptionErrorType));
    assertThat(exceptionMapper.resolveErrorType(Exception.class).isPresent(), is(false));
  }

  /**
   * Creates a complex hierarchy starting with {@link RuntimeException} and several children, but with gaps in between and adding
   * them out of order. Verifies that the error types respect the hierarchy even for those classes in the gap.
   */
  @Test
  public void complexHierarchyMapping() {
    ExceptionMapper exceptionMapper = ExceptionMapper.builder()
        .addExceptionMapping(NumberFormatException.class, numberFormatExceptionErrorType)
        .addExceptionMapping(ClassCastException.class, classCastExceptionErrorType)
        .addExceptionMapping(RuntimeException.class, runtimeExceptionErrorType)
        .addExceptionMapping(ArrayStoreChildException.class, arrayStoreChildExceptionErrorType)
        .addExceptionMapping(ArrayStoreException.class, arrayStoreExceptionErrorType)
        .build();

    assertThat(exceptionMapper.resolveErrorType(RuntimeException.class).get(), is(runtimeExceptionErrorType));
    assertThat(exceptionMapper.resolveErrorType(IllegalArgumentException.class).get(), is(runtimeExceptionErrorType));
    assertThat(exceptionMapper.resolveErrorType(NumberFormatException.class).get(), is(numberFormatExceptionErrorType));
    assertThat(exceptionMapper.resolveErrorType(ArrayStoreException.class).get(), is(arrayStoreExceptionErrorType));
    assertThat(exceptionMapper.resolveErrorType(ArrayStoreChildException.class).get(), is(arrayStoreChildExceptionErrorType));
    assertThat(exceptionMapper.resolveErrorType(ClassCastException.class).get(), is(classCastExceptionErrorType));
    assertThat(exceptionMapper.resolveErrorType(ClassCastChildException.class).get(), is(classCastExceptionErrorType));
    assertThat(exceptionMapper.resolveErrorType(Exception.class).isPresent(), is(false));
  }

  @Test(expected = MuleRuntimeException.class)
  public void sameExceptionWithDifferentErrorTypes() {
    ExceptionMapper.builder()
        .addExceptionMapping(NumberFormatException.class, numberFormatExceptionErrorType)
        .addExceptionMapping(NumberFormatException.class, illegalArgumentExceptionErrorType);
  }

  private static class ClassCastChildException extends ClassCastException {

  }

  private static class ArrayStoreChildException extends ArrayStoreException {

  }
}
