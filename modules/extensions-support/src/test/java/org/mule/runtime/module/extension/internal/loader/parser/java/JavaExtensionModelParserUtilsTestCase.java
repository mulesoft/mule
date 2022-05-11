/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.extension.api.annotation.execution.Execution;
import org.mule.runtime.module.extension.api.loader.java.type.OperationElement;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ClassBasedAnnotationValueFetcher;
import org.mule.tck.size.SmallTest;

import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@SmallTest
@RunWith(PowerMockRunner.class)
public class JavaExtensionModelParserUtilsTestCase {

  private OperationElement operationElement = mock(OperationElement.class);
  private ClassBasedAnnotationValueFetcher classBasedAnnotationValueFetcher = mock(ClassBasedAnnotationValueFetcher.class);

  @Test
  public void operationExecutionTypeFromLegacyAnnotation() {
    mockOperationExecutionTypeFromAnnotation(Execution.class,
                                             ExecutionType.CPU_LITE);
    Optional<ExecutionType> executionType = JavaExtensionModelParserUtils.getExecutionType(operationElement);
    assertThat(executionType.isPresent(), is(true));
    assertThat(executionType.get(), is(ExecutionType.CPU_LITE));
  }

  @Test
  public void operationExecutionTypeFromSdkApiAnnotation() {
    mockOperationExecutionTypeFromAnnotation(org.mule.sdk.api.annotation.execution.Execution.class,
                                             org.mule.sdk.api.runtime.operation.ExecutionType.CPU_LITE);
    Optional<ExecutionType> executionType = JavaExtensionModelParserUtils.getExecutionType(operationElement);
    assertThat(executionType.isPresent(), is(true));
    assertThat(executionType.get(), is(ExecutionType.CPU_LITE));
  }

  @Test
  @PrepareForTest(org.mule.sdk.api.runtime.operation.ExecutionType.class)
  public void operationExecutionTypeFromSdkApiAnnotationWithUndefinedExecutionType() {
    org.mule.sdk.api.runtime.operation.ExecutionType UNKNOWN_EXECUTION_TYPE =
        mock(org.mule.sdk.api.runtime.operation.ExecutionType.class);
    Whitebox.setInternalState(UNKNOWN_EXECUTION_TYPE, "name", "UNKNOWN");
    mockOperationExecutionTypeFromAnnotation(org.mule.sdk.api.annotation.execution.Execution.class,
                                             UNKNOWN_EXECUTION_TYPE);
    Optional<ExecutionType> executionType = JavaExtensionModelParserUtils.getExecutionType(operationElement);
    assertThat(executionType.isPresent(), is(false));
  }

  private void mockOperationExecutionTypeFromAnnotation(Class executionAnnotationClass, Enum executionType) {
    when(classBasedAnnotationValueFetcher.getEnumValue(any())).thenReturn(executionType);
    when(operationElement.getValueFromAnnotation(executionAnnotationClass))
        .thenReturn(Optional.of(classBasedAnnotationValueFetcher));
  }

}
