/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mule.runtime.extension.api.ExtensionConstants.TRANSACTIONAL_ACTION_PARAMETER_NAME;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.extension.internal.loader.validator.TransactionalParametersValidator;
import org.mule.runtime.extension.internal.property.TransactionalActionModelProperty;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Optional;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class TransactionalParametersValidatorTestCase {

  private final Optional<TransactionalActionModelProperty> transactionalActionModelProperty =
      Optional.of(new TransactionalActionModelProperty());

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  ExtensionModel extensionModel;

  @Mock
  private SourceModel sourceModel;

  @Mock
  OperationModel operationModel;

  @Mock
  ParameterGroupModel parameterGroupModel;

  @Mock
  ParameterModel txParameter;

  ExtensionModelValidator validator = new TransactionalParametersValidator();
  private ProblemsReporter problemsReporter;

  @Before
  public void setUp() {
    when(extensionModel.getSourceModels()).thenReturn(singletonList(sourceModel));
    when(extensionModel.getOperationModels()).thenReturn(singletonList(operationModel));

    when(sourceModel.getErrorCallback()).thenReturn(Optional.empty());
    when(sourceModel.getSuccessCallback()).thenReturn(Optional.empty());
    when(sourceModel.getParameterGroupModels()).thenReturn(singletonList(parameterGroupModel));
    when(sourceModel.isTransactional()).thenReturn(true);

    when(operationModel.isTransactional()).thenReturn(true);

    when(parameterGroupModel.getParameterModels()).thenReturn(singletonList(txParameter));

    when(txParameter.getName()).thenReturn(TRANSACTIONAL_ACTION_PARAMETER_NAME);
    when(txParameter.getModelProperty(TransactionalActionModelProperty.class)).thenReturn(transactionalActionModelProperty);

    problemsReporter = new ProblemsReporter(extensionModel);
  }

  @Test
  public void transactionalActionIsReservedWord() {
    when(txParameter.getModelProperty(any())).thenReturn(Optional.empty());
    validator.validate(extensionModel, problemsReporter);

    List<Problem> errors = problemsReporter.getErrors();
    assertThat(errors, hasSize(1));

    Problem problem = errors.get(0);
    assertThat(problem.getComponent(), is(sourceModel));
    assertThat(problem.getMessage(),
               containsString("defines a parameter named: 'transactionalAction', which is a reserved word"));
  }

  @Test
  public void transactionalParameterWithReservedWord() {
    validator.validate(extensionModel, problemsReporter);

    List<Problem> errors = problemsReporter.getErrors();
    assertThat(errors, empty());
  }

  @Test
  public void transactionalParameterCantBePlacedInParameterGroupWithShowInDsl() {
    when(parameterGroupModel.isShowInDsl()).thenReturn(true);
    validator.validate(extensionModel, problemsReporter);

    List<Problem> errors = problemsReporter.getErrors();
    assertThat(errors, hasSize(1));

    Problem problem = errors.get(0);
    assertThat(problem.getComponent(), is(sourceModel));
    assertThat(problem.getMessage(),
               containsString("Transactional parameters can't be placed inside of Parameter Groups with 'showInDsl' option."));
  }
}
