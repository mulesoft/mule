/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.extension.api.annotation.execution.OnError;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.annotation.execution.OnTerminate;
import org.mule.runtime.extension.api.annotation.source.OnBackPressure;
import org.mule.runtime.extension.api.exception.IllegalSourceModelDefinitionException;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.tck.size.SmallTest;

import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class SourceTypeWrapperTestCase {

  private static final String ON_SUCCESS_METHOD_NAME = "onSuccessMethod";
  private static final String ON_ERROR_METHOD_NAME = "onErrorMethod";
  private static final String ON_TERMINATE_METHOD_NAME = "onTerminateMethod";
  private static final String ON_BACK_PRESSURE_METHOD_NAME = "onBackPressureMethod";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  ClassTypeLoader classTypeLoader;

  private SourceTypeWrapper sourceTypeWrapperForLegacyApi;
  private SourceTypeWrapper sourceTypeWrapperForSdkApi;
  private SourceTypeWrapper sourceTypeWrapperWithMixedApis;
  private SourceTypeWrapper sourceTypeWrapperWithInvalidApiUsage;
  private SourceTypeWrapper sourceTypeWrapperWithInvalidOnBackPressureApiUsage;

  @Before
  public void before() {
    sourceTypeWrapperForLegacyApi = new SourceTypeWrapper(SourceTestWithLegacyAnnotations.class, classTypeLoader);
    sourceTypeWrapperForSdkApi = new SourceTypeWrapper(SourceTestWithSdkApiAnnotations.class, classTypeLoader);
    sourceTypeWrapperWithMixedApis = new SourceTypeWrapper(SourceTestWithMixedApiAnnotations.class, classTypeLoader);
    sourceTypeWrapperWithInvalidApiUsage =
        new SourceTypeWrapper(SourceTestWithInvalidAnnotationsUsage.class, classTypeLoader);
    sourceTypeWrapperWithInvalidOnBackPressureApiUsage =
        new SourceTypeWrapper(SourceTestWithInvalidOnBackPressureAnnotationsUsage.class, classTypeLoader);

  }

  @Test
  public void methodWithOnSuccessLegacyAnnotation() {
    verifyMethod(sourceTypeWrapperForLegacyApi.getOnResponseMethod(), ON_SUCCESS_METHOD_NAME);
  }

  @Test
  public void methodWithOnSuccessSdkApiAnnotation() {
    verifyMethod(sourceTypeWrapperForSdkApi.getOnResponseMethod(), ON_SUCCESS_METHOD_NAME);
  }

  @Test
  public void methodWithOnErrorLegacyAnnotation() {
    verifyMethod(sourceTypeWrapperForLegacyApi.getOnErrorMethod(), ON_ERROR_METHOD_NAME);
  }

  @Test
  public void methodWithOnErrorSdkApiAnnotation() {
    verifyMethod(sourceTypeWrapperForSdkApi.getOnErrorMethod(), ON_ERROR_METHOD_NAME);
  }

  @Test
  public void methodWithOnTerminateLegacyAnnotation() {
    verifyMethod(sourceTypeWrapperForLegacyApi.getOnTerminateMethod(), ON_TERMINATE_METHOD_NAME);
  }

  @Test
  public void methodWithOnTerminateSdkApiAnnotation() {
    verifyMethod(sourceTypeWrapperForSdkApi.getOnTerminateMethod(), ON_TERMINATE_METHOD_NAME);
  }

  @Test
  public void methodWithOnBackPressureLegacyAnnotation() {
    verifyMethod(sourceTypeWrapperForLegacyApi.getOnBackPressureMethod(), ON_BACK_PRESSURE_METHOD_NAME);
  }

  @Test
  public void methodWithOnBackPressureSdkApiAnnotation() {
    verifyMethod(sourceTypeWrapperForSdkApi.getOnBackPressureMethod(), ON_BACK_PRESSURE_METHOD_NAME);
  }

  @Test
  public void methodsWithMixedApiAnnotations() {
    verifyMethod(sourceTypeWrapperWithMixedApis.getOnResponseMethod(), ON_SUCCESS_METHOD_NAME);
    verifyMethod(sourceTypeWrapperWithMixedApis.getOnErrorMethod(), ON_ERROR_METHOD_NAME);
    verifyMethod(sourceTypeWrapperWithMixedApis.getOnTerminateMethod(), ON_TERMINATE_METHOD_NAME);
    verifyMethod(sourceTypeWrapperWithMixedApis.getOnBackPressureMethod(), ON_BACK_PRESSURE_METHOD_NAME);
  }

  @Test
  public void methodsWithInvalidAnnotationUsage() {
    expectedException.expect(IllegalSourceModelDefinitionException.class);
    sourceTypeWrapperWithInvalidApiUsage.getOnResponseMethod();
  }

  @Test
  public void methodsWithInvalidOnBackPressureAnnotationUsage() {
    expectedException.expect(IllegalSourceModelDefinitionException.class);
    sourceTypeWrapperWithInvalidOnBackPressureApiUsage.getOnBackPressureMethod();
  }

  private void verifyMethod(Optional<MethodElement> method, String methodName) {
    assertThat(method.isPresent(), is(true));
    assertThat(method.get().getName(), is(methodName));
  }

  private class SourceTestWithLegacyAnnotations {

    @OnSuccess
    public void onSuccessMethod() {}

    @OnError
    public void onErrorMethod() {}

    @OnTerminate
    public void onTerminateMethod() {}

    @OnBackPressure
    public void onBackPressureMethod() {}
  }


  private class SourceTestWithSdkApiAnnotations {

    @org.mule.sdk.api.annotation.execution.OnSuccess
    public void onSuccessMethod() {}

    @org.mule.sdk.api.annotation.execution.OnError
    public void onErrorMethod() {}

    @org.mule.sdk.api.annotation.execution.OnTerminate
    public void onTerminateMethod() {}

    @org.mule.sdk.api.annotation.source.OnBackPressure
    public void onBackPressureMethod() {}
  }


  private class SourceTestWithMixedApiAnnotations {

    @OnSuccess
    public void onSuccessMethod() {}

    @org.mule.sdk.api.annotation.execution.OnError
    public void onErrorMethod() {}

    @OnTerminate
    public void onTerminateMethod() {}

    @org.mule.sdk.api.annotation.source.OnBackPressure
    public void onBackPressureMethod() {}
  }


  private class SourceTestWithInvalidAnnotationsUsage {

    @OnSuccess
    @org.mule.sdk.api.annotation.execution.OnSuccess
    public void onSuccessMethod() {}
  }

  private class SourceTestWithInvalidOnBackPressureAnnotationsUsage {

    @OnBackPressure
    @org.mule.sdk.api.annotation.source.OnBackPressure
    public void onBackPressure() {}
  }
}
