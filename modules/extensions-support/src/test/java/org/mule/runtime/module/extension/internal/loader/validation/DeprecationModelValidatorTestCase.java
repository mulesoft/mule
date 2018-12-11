/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.extension.api.model.deprecated.ImmutableDeprecationModel;
import org.mule.runtime.module.extension.internal.loader.java.property.CompileTimeModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;


@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DeprecationModelValidatorTestCase extends AbstractMuleTestCase {

  private DeprecationModelValidator validator = new DeprecationModelValidator();

  @Mock(lenient = true)
  private ParameterModel parameterModel;

  @Mock(lenient = true)
  private OperationModel operationModel;

  @Mock(lenient = true)
  private ParameterGroupModel parameterGroupModel;

  @Mock(lenient = true)
  private ExtensionModel extensionModel;

  @Before
  public void doSetup() {
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    when(extensionModel.getConfigurationModels()).thenReturn(emptyList());
    when(extensionModel.getConstructModels()).thenReturn(emptyList());
    when(extensionModel.getFunctionModels()).thenReturn(emptyList());
    when(extensionModel.getConnectionProviders()).thenReturn(emptyList());
    when(extensionModel.getSourceModels()).thenReturn(emptyList());
    when(extensionModel.getDeprecationModel()).thenReturn(empty());
    when(extensionModel.getName()).thenReturn("extensionName");
    when(operationModel.getParameterGroupModels()).thenReturn(asList(parameterGroupModel));
    when(operationModel.getDeprecationModel()).thenReturn(empty());
    when(operationModel.getName()).thenReturn("extensionOperation");
    when(parameterGroupModel.getParameterModels()).thenReturn(asList(parameterModel));
    when(parameterModel.isDeprecated()).thenReturn(false);
    when(parameterModel.getDeprecationModel()).thenReturn(empty());
    when(parameterModel.getName()).thenReturn("parameterName");


    when(extensionModel.getModelProperty(CompileTimeModelProperty.class)).thenReturn(ofNullable(new CompileTimeModelProperty()));
  }

  @Test
  public void deprecatedExtensionWithInvalidVersion() {
    when(extensionModel.isDeprecated()).thenReturn(true);
    when(extensionModel.getDeprecationModel())
        .thenReturn(of(new ImmutableDeprecationModel("This extension is deprecated.", "hola", null)));

    ProblemsReporter problemsReporter = new ProblemsReporter(extensionModel);
    validator.validate(extensionModel, problemsReporter);
    assertTrue(problemsReporter.hasErrors());

    assertThat(problemsReporter.getErrors().get(0).getMessage(),
               is("The extension named extensionName was deprecated with an invalid 'since' version : 'hola' . This version must follow the semver convention"));
  }

  @Test
  public void optionalDeprecatedParameter() {
    when(parameterModel.isDeprecated()).thenReturn(true);
    ProblemsReporter problemsReporter = new ProblemsReporter(extensionModel);
    when(parameterModel.isRequired()).thenReturn(false);
    validator.validate(extensionModel, problemsReporter);
    assertFalse(problemsReporter.hasErrors());
  }

  @Test
  public void requiredDeprecatedParameter() {
    when(parameterModel.isDeprecated()).thenReturn(true);
    ProblemsReporter problemsReporter = new ProblemsReporter(extensionModel);
    when(parameterModel.isRequired()).thenReturn(true);
    validator.validate(extensionModel, problemsReporter);
    assertTrue(problemsReporter.hasErrors());
  }

  @Test
  public void deprecatedParameterWithInvalidVersion() {
    when(parameterModel.isDeprecated()).thenReturn(true);
    when(parameterModel.isRequired()).thenReturn(false);
    when(parameterModel.getDeprecationModel())
        .thenReturn(of(new ImmutableDeprecationModel("This parameter is deprecated", "2.hola", null)));
    ProblemsReporter problemsReporter = new ProblemsReporter(extensionModel);
    validator.validate(extensionModel, problemsReporter);
    assertTrue(problemsReporter.hasErrors());
    assertThat(problemsReporter.getErrors().get(0).getMessage(),
               is("The parameter named parameterName was deprecated with an invalid 'since' version : '2.hola' . This version must follow the semver convention"));
  }

  @Test
  public void deprecatedOperationWithInvalidVersion() {
    when(operationModel.isDeprecated()).thenReturn(true);
    when(operationModel.getDeprecationModel())
        .thenReturn(of(new ImmutableDeprecationModel("This operation is deprecated", "2.hola", null)));
    ProblemsReporter problemsReporter = new ProblemsReporter(extensionModel);
    validator.validate(extensionModel, problemsReporter);
    assertTrue(problemsReporter.hasErrors());
    assertThat(problemsReporter.getErrors().get(0).getMessage(),
               is("The operation named extensionOperation was deprecated with an invalid 'since' version : '2.hola' . This version must follow the semver convention"));
  }

  @Test
  public void deprecatedOperationWithValidVersions() {
    when(operationModel.isDeprecated()).thenReturn(true);
    when(operationModel.getDeprecationModel())
        .thenReturn(of(new ImmutableDeprecationModel("This operation is deprecated", "2.2.3", "3.0.0")));
    ProblemsReporter problemsReporter = new ProblemsReporter(extensionModel);
    validator.validate(extensionModel, problemsReporter);
    assertFalse(problemsReporter.hasErrors());
  }

  @Test
  public void deprecatedOperationWithToRemoveInPriorToSinceVersion() {
    when(operationModel.isDeprecated()).thenReturn(true);
    when(operationModel.getDeprecationModel())
        .thenReturn(of(new ImmutableDeprecationModel("This operation is deprecated", "2.2.0", "2.0.0")));
    ProblemsReporter problemsReporter = new ProblemsReporter(extensionModel);
    validator.validate(extensionModel, problemsReporter);
    assertTrue(problemsReporter.hasErrors());
    assertThat(problemsReporter.getErrors().get(0).getMessage(),
               is("The versions chosen for the deprecation of the operation named extensionOperation are invalid, `since`(2.2.0) version must be prior to the `removeTo`(2.0.0) version."));
  }

}
