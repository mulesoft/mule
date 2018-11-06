/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;


import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.module.extension.internal.loader.java.property.CompileTimeModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DeprecationModelValidatorTestCase extends AbstractMuleTestCase {

  private DeprecationModelValidator validator = new DeprecationModelValidator();

  @Mock
  private ParameterModel parameterModel;

  @Mock
  private OperationModel operationModel;

  @Mock
  private ParameterGroupModel parameterGroupModel;

  @Mock
  private ExtensionModel extensionModel;

  @Before
  public void doSetup() {
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    when(extensionModel.getConfigurationModels()).thenReturn(emptyList());
    when(extensionModel.getConstructModels()).thenReturn(emptyList());
    when(extensionModel.getFunctionModels()).thenReturn(emptyList());
    when(extensionModel.getConnectionProviders()).thenReturn(emptyList());
    when(extensionModel.getSourceModels()).thenReturn(emptyList());
    when(operationModel.getParameterGroupModels()).thenReturn(asList(parameterGroupModel));
    when(parameterGroupModel.getParameterModels()).thenReturn(asList(parameterModel));
    when(parameterModel.isDeprecated()).thenReturn(true);

    when(extensionModel.getModelProperty(CompileTimeModelProperty.class)).thenReturn(ofNullable(new CompileTimeModelProperty()));
  }

  @Test
  public void validDeprecatedOperation() {
    ProblemsReporter problemsReporter = new ProblemsReporter(extensionModel);
    when(parameterModel.isRequired()).thenReturn(false);
    validator.validate(extensionModel, problemsReporter);
    assertFalse(problemsReporter.hasErrors());
  }

  @Test
  public void invalidDeprecatedOperation() {
    ProblemsReporter problemsReporter = new ProblemsReporter(extensionModel);
    when(parameterModel.isRequired()).thenReturn(true);
    validator.validate(extensionModel, problemsReporter);
    assertTrue(problemsReporter.hasErrors());
  }

}
