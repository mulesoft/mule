/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.util.Arrays.asList;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;
import org.mule.runtime.api.message.NullAttributes;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.internal.loader.validator.OperationParametersModelValidator;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.module.extension.internal.util.ExtensionsTestUtils;

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

  @Mock(answer = RETURNS_DEEP_STUBS)
  private OperationModel operationModel;

  @Mock
  private ParameterModel goodParameter;

  private ExtensionModelValidator validator = new OperationParametersModelValidator();

  @Before
  public void before() {
    when(extensionModel.getName()).thenReturn(EXTENSION_NAME);
    when(operationModel.getName()).thenReturn(OPERATION_NAME);
    when(operationModel.getOutput().getType()).thenReturn(TYPE_LOADER.load(String.class));
    when(operationModel.getOutputAttributes().getType()).thenReturn(TYPE_LOADER.load(NullAttributes.class));
    when(goodParameter.getName()).thenReturn("valid");
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    when(operationModel.getAllParameterModels()).thenReturn(asList(goodParameter));
  }

  private void validate() {
    ExtensionsTestUtils.validate(extensionModel, validator);
  }

  @Test
  public void valid() {
    validate();
  }

  @Test
  public void validForParameterLessOperation() {
    when(operationModel.getAllParameterModels()).thenReturn(ImmutableList.of());
    validate();
  }
}
