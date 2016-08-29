/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import org.mule.runtime.api.message.ErrorType;
import org.mule.tck.junit4.AbstractMuleTestCase;

public class ExceptionMapperTestCase extends AbstractMuleTestCase {

  private ErrorType runtimeExceptionErrorType = mock(ErrorType.class);
  private ErrorType illegalArgumentExceptionErrorType = mock(ErrorType.class);
  private ErrorType numberFormatExceptionErrorType = mock(ErrorType.class);

  private ErrorType classCastExceptionErrorType = mock(ErrorType.class);
  private ErrorType arrayStoreExceptionErrorType = mock(ErrorType.class);

  @Test
  public void sameHierarchyMapping() {
    ExceptionMapper exceptionMapper = ExceptionMapper.builder()
        .addExceptionMapping(RuntimeException.class, runtimeExceptionErrorType)
        .addExceptionMapping(NumberFormatException.class, numberFormatExceptionErrorType)
        .addExceptionMapping(IllegalArgumentException.class, illegalArgumentExceptionErrorType).build();

    assertThat(exceptionMapper.resolveErrorType(new IllegalArgumentException()).get(), is(illegalArgumentExceptionErrorType));
    assertThat(exceptionMapper.resolveErrorType(new NumberFormatException()).get(), is(numberFormatExceptionErrorType));
    assertThat(exceptionMapper.resolveErrorType(new RuntimeException()).get(), is(runtimeExceptionErrorType));
    assertThat(exceptionMapper.resolveErrorType(new Exception()).isPresent(), is(false));
  }

  @Test
  public void differentHierarchyMapping() {
    ExceptionMapper exceptionMapper = ExceptionMapper.builder()
        .addExceptionMapping(NumberFormatException.class, numberFormatExceptionErrorType)
        .addExceptionMapping(ClassCastException.class, classCastExceptionErrorType)
        .addExceptionMapping(ArrayStoreException.class, arrayStoreExceptionErrorType).build();

    assertThat(exceptionMapper.resolveErrorType(new NumberFormatException()).get(), is(numberFormatExceptionErrorType));
    assertThat(exceptionMapper.resolveErrorType(new ArrayStoreException()).get(), is(arrayStoreExceptionErrorType));
    assertThat(exceptionMapper.resolveErrorType(new ClassCastException()).get(), is(classCastExceptionErrorType));
    assertThat(exceptionMapper.resolveErrorType(new Exception()).isPresent(), is(false));
  }

}
