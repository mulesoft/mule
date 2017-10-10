/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceCallbackModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.extension.api.annotation.execution.OnTerminate;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class SourceCallbacksModelValidatorTestCase extends AbstractMuleTestCase {

  @Mock
  ExtensionModel extensionModel;

  @Mock
  SourceModel sourceModel;

  @Mock
  ParameterModel invalidParameter;

  @Mock
  SourceCallbackModel onTerminateCallback;

  @Mock
  SourceCallbackModel onErrorCallback;

  @Mock
  SourceCallbackModel onSuccessCallback;

  private ProblemsReporter problemsReporter;
  private SourceCallbacksModelValidator validator;

  @Before
  public void setUp() throws Exception {
    problemsReporter = new ProblemsReporter(extensionModel);
    validator = new SourceCallbacksModelValidator();
    when(extensionModel.getSourceModels()).thenReturn(singletonList(sourceModel));
    when(sourceModel.getTerminateCallback()).thenReturn(of(onTerminateCallback));
    when(sourceModel.getSuccessCallback()).thenReturn(Optional.empty());
    when(sourceModel.getErrorCallback()).thenReturn(Optional.empty());
  }

  @Test
  public void onTerminateWithInvalidParameter() {
    when(onTerminateCallback.getAllParameterModels()).thenReturn(singletonList(invalidParameter));
    validator.validate(extensionModel, problemsReporter);

    assertProblemContaining("@OnTerminate callback method can only receive parameters of the following types: "
        + "'SourceResult' and 'SourceCallbackContext'");
  }

  @Test
  public void onTerminateWithValidParameters() {
    when(onTerminateCallback.getAllParameterModels()).thenReturn(emptyList());
    validator.validate(extensionModel, problemsReporter);

    assertThat(problemsReporter.getErrors(), is(empty()));
  }

  @Test
  public void sourcesWithCallbacksShouldDefineOnTerminate() {
    when(sourceModel.getTerminateCallback()).thenReturn(Optional.empty());
    when(sourceModel.getSuccessCallback()).thenReturn(of(onSuccessCallback));
    validator.validate(extensionModel, problemsReporter);

    assertOnTerminateRequired();
  }

  @Test
  public void sourcesWithOnErrorCallbackWithParameterShouldDefineOnTerminate() {
    when(sourceModel.getTerminateCallback()).thenReturn(Optional.empty());
    when(sourceModel.getErrorCallback()).thenReturn(of(onErrorCallback));
    when(onErrorCallback.getAllParameterModels()).thenReturn(singletonList(invalidParameter));
    validator.validate(extensionModel, problemsReporter);

    assertOnTerminateRequired();
  }

  private void assertOnTerminateRequired() {
    assertProblemContaining(String.format("another method annotated with @%s is required", OnTerminate.class.getSimpleName()));
  }

  @Test
  public void sourcesWithParameterlessOnErrorCallback() {
    when(sourceModel.getTerminateCallback()).thenReturn(Optional.empty());
    when(sourceModel.getSuccessCallback()).thenReturn(of(onSuccessCallback));
    when(sourceModel.getErrorCallback()).thenReturn(of(onErrorCallback));
    validator.validate(extensionModel, problemsReporter);

    List<Problem> errors = problemsReporter.getErrors();
    assertThat(errors, is(empty()));
  }

  private void assertProblemContaining(String substring) {
    List<Problem> errors = problemsReporter.getErrors();
    assertThat(errors, is(not(empty())));
    Problem problem = errors.get(0);
    assertThat(problem.getComponent(), is(sourceModel));
    assertThat(problem.getMessage(),
               is(containsString(substring)));
  }
}
