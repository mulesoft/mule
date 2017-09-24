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
import static org.mule.runtime.api.util.ExtensionModelTestUtils.visitableMock;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockParameters;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.validate;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class OperationParametersTypeModelValidatorTestCase extends AbstractMuleTestCase {

  public static final String EXTENSION_NAME = "extension";
  public static final String OPERATION_NAME = "operation";
  @Mock
  private ExtensionModel extensionModel;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private OperationModel operationModel;

  @Mock
  private ParameterModel parameterModel;

  private ExtensionModelValidator validator = new OperationParametersTypeModelValidator();

  @Before
  public void before() {
    when(extensionModel.getName()).thenReturn(EXTENSION_NAME);
    when(operationModel.getName()).thenReturn(OPERATION_NAME);
    when(parameterModel.getName()).thenReturn("parameterName");
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    mockParameters(operationModel, parameterModel);
    visitableMock(operationModel);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void eventType() {
    when(parameterModel.getType()).thenReturn(toMetadataType(CoreEvent.class));
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void messageType() {
    when(parameterModel.getType()).thenReturn(toMetadataType(Message.class));
    validate(extensionModel, validator);
  }

  @Test
  public void validType() {
    when(parameterModel.getType()).thenReturn(toMetadataType(Object.class));
    validate(extensionModel, validator);
  }
}
