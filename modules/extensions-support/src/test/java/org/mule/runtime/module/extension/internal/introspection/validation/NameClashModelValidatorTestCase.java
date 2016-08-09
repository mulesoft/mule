/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static java.util.Arrays.asList;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.getParameter;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.connection.ConnectionProviderModel;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class NameClashModelValidatorTestCase extends AbstractMuleTestCase {

  private static final String OPERATION_NAME = "operation";
  private static final String TOP_LEVEL_OPERATION_PARAM_NAME = "topLevelOperationParam";
  private static final String CONFIG_NAME = "config";
  private static final String CONNECTION_PROVIDER_NAME = "connectionProvider";
  private static final String SIMPLE_PARAM_NAME = "simple";

  @Mock
  private ExtensionModel extensionModel;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private RuntimeConfigurationModel configurationModel;

  @Mock
  private OperationModel operationModel;

  @Mock
  private ConnectionProviderModel connectionProviderModel;

  private ParameterModel simpleConfigParam;
  private ParameterModel topLevelConfigParam;
  private ParameterModel simpleOperationParam;
  private ParameterModel topLevelOperationParam;
  private ParameterModel simpleConnectionProviderParam;
  private ParameterModel topLevelConnectionProviderParam;

  private NameClashModelValidator validator = new NameClashModelValidator();

  @Before
  public void before() {
    when(extensionModel.getName()).thenReturn("extensionName");
    when(extensionModel.getConfigurationModels()).thenReturn(asList(configurationModel));
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    when(extensionModel.getConnectionProviders()).thenReturn(asList(connectionProviderModel));

    simpleConfigParam = getParameter(SIMPLE_PARAM_NAME, String.class);
    topLevelConfigParam = getParameter("topLevelConfigParam", Apple.class);
    simpleOperationParam = getParameter(SIMPLE_PARAM_NAME, String.class);
    topLevelOperationParam = getParameter(TOP_LEVEL_OPERATION_PARAM_NAME, Apple.class);
    simpleConnectionProviderParam = getParameter(SIMPLE_PARAM_NAME, String.class);
    topLevelConnectionProviderParam = getParameter("topLevelConnectionProviderParam", Apple.class);

    when(configurationModel.getName()).thenReturn(CONFIG_NAME);
    when(configurationModel.getParameterModels()).thenReturn(asList(simpleConfigParam, topLevelConfigParam));
    when(configurationModel.getOperationModels()).thenReturn(ImmutableList.of());
    when(configurationModel.getConnectionProviders()).thenReturn(ImmutableList.of());

    when(operationModel.getName()).thenReturn(OPERATION_NAME);
    when(operationModel.getParameterModels()).thenReturn(asList(simpleOperationParam, topLevelOperationParam));
    when(connectionProviderModel.getName()).thenReturn(CONNECTION_PROVIDER_NAME);
    when(connectionProviderModel.getParameterModels())
        .thenReturn(asList(simpleConnectionProviderParam, topLevelConnectionProviderParam));
  }

  @Test
  public void validModel() {
    validate();
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void operationClashingWithConfig() {
    when(configurationModel.getName()).thenReturn(OPERATION_NAME);
    validate();
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void connectionProviderClashesWithConfig() {
    when(connectionProviderModel.getName()).thenReturn(CONFIG_NAME);
    validate();
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void configTopLevelParameterCrashesWithOperationName() {
    ParameterModel offending = getParameter(TOP_LEVEL_OPERATION_PARAM_NAME, Banana.class);
    when(configurationModel.getParameterModels()).thenReturn(asList(simpleConfigParam, topLevelConfigParam, offending));
    validate();
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void configNameClashesWithOperationParameterType() {
    ParameterModel offending = getParameter(SIMPLE_PARAM_NAME, Banana.class);
    when(configurationModel.getName()).thenReturn(Banana.class.getName());
    when(operationModel.getParameterModels()).thenReturn(asList(topLevelOperationParam, offending));
    validate();
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void connectionProviderTopLevelParameterCrashesWithOperationName() {
    ParameterModel offending = getParameter(TOP_LEVEL_OPERATION_PARAM_NAME, Banana.class);
    when(connectionProviderModel.getParameterModels())
        .thenReturn(asList(simpleConnectionProviderParam, topLevelConnectionProviderParam, offending));
    validate();
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void connectionProviderNameClashesWithOperationParameterName() {
    ParameterModel offending = getParameter(SIMPLE_PARAM_NAME, Banana.class);
    when(connectionProviderModel.getName()).thenReturn(SIMPLE_PARAM_NAME);
    when(operationModel.getParameterModels()).thenReturn(asList(topLevelOperationParam, offending));
    validate();
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void connectionProviderNameClashesWithOperationParameterType() {
    ParameterModel offending = getParameter(connectionProviderModel.getName(), Banana.class);
    when(operationModel.getParameterModels()).thenReturn(asList(topLevelOperationParam, offending));
    validate();
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void configTopLevelParameterCrashesWithConfigName() {
    ParameterModel offending = getParameter(CONFIG_NAME, Apple.class);
    when(configurationModel.getParameterModels()).thenReturn(asList(simpleConfigParam, topLevelConfigParam, offending));
    validate();
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void connectionProviderTopLevelParameterCrashesWithConfigName() {
    ParameterModel offending = getParameter(CONFIG_NAME, Apple.class);
    when(connectionProviderModel.getParameterModels())
        .thenReturn(asList(simpleConnectionProviderParam, topLevelConnectionProviderParam, offending));
    validate();
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void operationTopLevelParameterCrashesWithOperationName() {
    ParameterModel offending = getParameter(OPERATION_NAME, Apple.class);
    when(operationModel.getParameterModels()).thenReturn(asList(simpleOperationParam, topLevelOperationParam, offending));
    validate();
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void connectionProviderTopLevelParameterCrashesWithName() {
    ParameterModel offending = getParameter(CONNECTION_PROVIDER_NAME, Apple.class);
    when(connectionProviderModel.getParameterModels())
        .thenReturn(asList(simpleConnectionProviderParam, topLevelConnectionProviderParam, offending));
    validate();
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void configWithRepeatedParameterName() {
    ParameterModel offending = getParameter(SIMPLE_PARAM_NAME, String.class);
    when(configurationModel.getParameterModels()).thenReturn(asList(simpleConfigParam, topLevelConfigParam, offending));
    validate();
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void operationWithRepeatedParameterName() {
    ParameterModel offending = getParameter(SIMPLE_PARAM_NAME, String.class);
    when(operationModel.getParameterModels()).thenReturn(asList(simpleOperationParam, topLevelOperationParam, offending));
    validate();
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void operationNameClashesWithParameterTypeName() {
    ParameterModel offending = getParameter(SIMPLE_PARAM_NAME, Banana.class);
    when(operationModel.getName()).thenReturn(Banana.class.getName());
    when(operationModel.getParameterModels()).thenReturn(asList(topLevelOperationParam, offending));
    validate();
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void connectionProviderWithRepeatedParameterName() {
    ParameterModel offending = getParameter(SIMPLE_PARAM_NAME, String.class);
    when(connectionProviderModel.getParameterModels())
        .thenReturn(asList(simpleConnectionProviderParam, topLevelConnectionProviderParam, offending));
    validate();
  }

  private void validate() {
    validator.validate(extensionModel);
  }

}
