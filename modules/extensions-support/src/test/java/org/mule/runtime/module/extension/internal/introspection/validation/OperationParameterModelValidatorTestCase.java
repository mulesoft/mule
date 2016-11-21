/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.TARGET_ATTRIBUTE;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class OperationParameterModelValidatorTestCase extends AbstractMuleTestCase {

  public static final String EXTENSION_NAME = "extension";
  public static final String OPERATION_NAME = "operation";
  @Mock
  private ExtensionModel extensionModel;

  @Mock
  private OperationModel operationModel;

  @Mock
  private ParameterModel goodParameter;

  private ModelValidator validator = new OperationParametersModelValidator();

  @Before
  public void before() {
    when(extensionModel.getName()).thenReturn(EXTENSION_NAME);
    when(operationModel.getName()).thenReturn(OPERATION_NAME);
    when(goodParameter.getName()).thenReturn("valid");
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    when(operationModel.getParameterModels()).thenReturn(asList(goodParameter));
  }

  @Test
  public void valid() {
    validator.validate(extensionModel);
  }

  @Test(expected = IllegalOperationModelDefinitionException.class)
  public void targetParameter() {
    ParameterModel offending = mock(ParameterModel.class);
    when(offending.getName()).thenReturn(TARGET_ATTRIBUTE);

    when(operationModel.getParameterModels()).thenReturn(asList(goodParameter, offending));
    validator.validate(extensionModel);
  }

  @Test
  public void validForParameterLessOperation() {
    when(operationModel.getParameterModels()).thenReturn(ImmutableList.of());
    validator.validate(extensionModel);
  }
}
