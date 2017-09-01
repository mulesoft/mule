/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.internal.loader.validator.OperationModelValidator;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.module.extension.internal.util.ExtensionsTestUtils;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class OperationModelValidatorTestCase extends AbstractMuleTestCase {

  public static final String EXTENSION_NAME = "extension";
  public static final String OPERATION_NAME = "operation";

  @Rule
  public ExpectedException expectedException = none();

  @Mock
  private ExtensionModel extensionModel;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ConfigurationModel configurationModel;


  @Mock(answer = RETURNS_DEEP_STUBS)
  private OperationModel operationModel;

  @Mock
  private ParameterModel goodParameter;

  private ExtensionModelValidator validator = new OperationModelValidator();

  @Before
  public void before() {
    when(extensionModel.getName()).thenReturn(EXTENSION_NAME);
    when(operationModel.getName()).thenReturn(OPERATION_NAME);
    when(operationModel.getOutput().getType()).thenReturn(TYPE_LOADER.load(String.class));
    when(operationModel.getOutputAttributes().getType()).thenReturn(TYPE_LOADER.load(void.class));
    when(goodParameter.getName()).thenReturn("valid");
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    when(operationModel.getAllParameterModels()).thenReturn(asList(goodParameter));
  }

  private void validate() {
    ExtensionsTestUtils.validate(extensionModel, validator);
  }

  @Test
  public void validOperationParameters() {
    validate();
  }

  @Test
  public void globalConnectedOperationWithGlobalConnectionProvider() {
    when(operationModel.requiresConnection()).thenReturn(true);
    when(extensionModel.getConnectionProviders()).thenReturn(singletonList(mock(ConnectionProviderModel.class)));
    validate();
  }

  @Test
  public void globalConnectedOperationWithoutConnectionProvider() {
    when(operationModel.requiresConnection()).thenReturn(true);
    expectedException.expect(IllegalModelDefinitionException.class);

    validate();
  }

  @Test
  public void configLevelOperationWithGlobalConnectionProvider() {
    when(operationModel.requiresConnection()).thenReturn(true);
    when(extensionModel.getOperationModels()).thenReturn(emptyList());
    when(configurationModel.getOperationModels()).thenReturn(singletonList(operationModel));
    when(extensionModel.getConnectionProviders()).thenReturn(singletonList(mock(ConnectionProviderModel.class)));

    validate();
  }

  @Test
  public void configLevelOperationWithoutConnectionProvider() {
    when(operationModel.requiresConnection()).thenReturn(true);
    when(extensionModel.getOperationModels()).thenReturn(emptyList());
    when(configurationModel.getOperationModels()).thenReturn(singletonList(operationModel));
    when(configurationModel.getConnectionProviders()).thenReturn(emptyList());
    when(extensionModel.getConfigurationModels()).thenReturn(singletonList(configurationModel));

    expectedException.expect(IllegalModelDefinitionException.class);
    validate();
  }

  @Test
  public void configLevelOperationWithConfigLevelConnectionProvider() {
    when(operationModel.requiresConnection()).thenReturn(true);
    when(extensionModel.getOperationModels()).thenReturn(emptyList());
    when(configurationModel.getConnectionProviders()).thenReturn(singletonList(mock(ConnectionProviderModel.class)));
    when(configurationModel.getOperationModels()).thenReturn(singletonList(operationModel));

    validate();
  }

  @Test
  public void validForParameterLessOperation() {
    when(operationModel.getAllParameterModels()).thenReturn(ImmutableList.of());
    validate();
  }
}
