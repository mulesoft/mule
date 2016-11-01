/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.getParameter;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.model.property.ConfigTypeModelProperty;
import org.mule.runtime.extension.api.model.property.ConnectivityModelProperty;
import org.mule.runtime.extension.api.model.property.PagedOperationModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Banana;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class NameClashModelValidatorTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private static final String OPERATION_NAME = "operation";
  private static final String TOP_LEVEL_OPERATION_PARAM_NAME = "topLevelOperationParam";
  private static final String CONFIG_NAME = "config";
  private static final String CONNECTION_PROVIDER_NAME = "connectionProvider";
  private static final String SIMPLE_PARAM_NAME = "simple";

  private static final String TOP_LEVEL_PLURAL_PARAM_NAME = "topLevelTests";
  private static final String TOP_LEVEL_SINGULAR_PARAM_NAME = "topLevelTest";

  private static final String CHILD_PLURAL_PARAM_NAME = "childTests";
  private static final String CHILD_SINGULAR_PARAM_NAME = "childTest";

  @Mock
  private ExtensionModel extensionModel;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ConfigurationModel configurationModel;

  @Mock
  private OperationModel operationModel;

  @Mock
  private ConnectionProviderModel connectionProviderModel;

  private XmlDslModel xmlDslModel = new XmlDslModel();
  private ParameterModel simpleConfigParam;
  private ParameterModel topLevelConfigParam;
  private ParameterModel simpleOperationParam;
  private ParameterModel topLevelOperationParam;
  private ParameterModel simpleConnectionProviderParam;
  private ParameterModel topLevelConnectionProviderParam;
  private BaseTypeBuilder baseTypeBuilder = BaseTypeBuilder.create(JAVA);

  private MetadataType childTestList = baseTypeBuilder.arrayType()
      .id(ArrayList.class.getName()).of(toMetadataType(ChildTest.class)).build();

  private MetadataType topLevelTestList = baseTypeBuilder.arrayType()
      .id(ArrayList.class.getName()).of(toMetadataType(TopLevelTest.class)).build();

  private MetadataType childTestMap = baseTypeBuilder.dictionaryType()
      .id(HashMap.class.getName()).ofKey(toMetadataType(String.class)).ofValue(toMetadataType(ChildTest.class)).build();

  private MetadataType topLevelMap = baseTypeBuilder.dictionaryType()
      .id(HashMap.class.getName()).ofKey(toMetadataType(String.class)).ofValue(toMetadataType(TopLevelTest.class)).build();

  private NameClashModelValidator validator = new NameClashModelValidator();

  @Before
  public void before() {
    when(extensionModel.getName()).thenReturn("extensionName");
    when(extensionModel.getConfigurationModels()).thenReturn(singletonList(configurationModel));
    when(extensionModel.getOperationModels()).thenReturn(singletonList(operationModel));
    when(extensionModel.getConnectionProviders()).thenReturn(singletonList(connectionProviderModel));
    when(extensionModel.getXmlDslModel()).thenReturn(xmlDslModel);

    simpleConfigParam = getParameter(SIMPLE_PARAM_NAME, String.class);
    topLevelConfigParam = getParameter(TOP_LEVEL_OPERATION_PARAM_NAME, TopLevelTest.class);

    simpleOperationParam = getParameter(SIMPLE_PARAM_NAME, String.class);
    topLevelOperationParam = getParameter(TOP_LEVEL_OPERATION_PARAM_NAME, TopLevelTest.class);

    simpleConnectionProviderParam = getParameter(SIMPLE_PARAM_NAME, String.class);
    topLevelConnectionProviderParam = getParameter(TOP_LEVEL_OPERATION_PARAM_NAME, TopLevelTest.class);

    when(configurationModel.getName()).thenReturn(CONFIG_NAME);
    when(configurationModel.getParameterModels()).thenReturn(asList(simpleConfigParam, topLevelConfigParam));
    when(configurationModel.getOperationModels()).thenReturn(ImmutableList.of());
    when(configurationModel.getConnectionProviders()).thenReturn(ImmutableList.of());

    when(operationModel.getName()).thenReturn(OPERATION_NAME);
    when(operationModel.getParameterModels()).thenReturn(asList(simpleOperationParam, topLevelOperationParam));
    mockModelProperties(operationModel);
    mockModelProperties(configurationModel);
    mockModelProperties(connectionProviderModel);
    when(connectionProviderModel.getName()).thenReturn(CONNECTION_PROVIDER_NAME);
    when(connectionProviderModel.getParameterModels())
        .thenReturn(asList(simpleConnectionProviderParam, topLevelConnectionProviderParam));
  }

  private void mockModelProperties(EnrichableModel model) {
    when(model.getModelProperty(ConfigTypeModelProperty.class)).thenReturn(empty());
    when(model.getModelProperty(ConnectivityModelProperty.class)).thenReturn(empty());
    when(model.getModelProperty(PagedOperationModelProperty.class)).thenReturn(empty());
  }

  @Test
  public void validModel() {
    validate();
  }

  @Test
  public void operationClashingWithConfig() {
    exception.expect(IllegalModelDefinitionException.class);
    when(configurationModel.getName()).thenReturn(OPERATION_NAME);
    validate();
  }

  @Test
  public void connectionProviderClashesWithConfig() {
    exception.expect(IllegalModelDefinitionException.class);
    when(connectionProviderModel.getName()).thenReturn(CONFIG_NAME);
    validate();
  }

  @Test
  public void configParameterWithRepeatedName() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(TOP_LEVEL_OPERATION_PARAM_NAME, Banana.class);
    when(configurationModel.getParameterModels()).thenReturn(asList(simpleConfigParam, topLevelConfigParam, offending));
    validate();
  }

  @Test
  public void configNameClashesWithOperationParameterType() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(SIMPLE_PARAM_NAME, TopLevelTest.class);
    when(configurationModel.getName()).thenReturn(TopLevelTest.class.getName());
    when(operationModel.getParameterModels()).thenReturn(asList(topLevelOperationParam, offending));
    validate();
  }

  @Test
  public void connectionProviderTopLevelParameterCrashesWithOperationName() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(OPERATION_NAME, TopLevelTest.class);
    when(connectionProviderModel.getParameterModels())
        .thenReturn(asList(simpleConnectionProviderParam, topLevelConnectionProviderParam, offending));
    validate();
  }

  @Test
  public void connectionProviderNameClashesWithOperationParameterName() {
    exception.expect(IllegalModelDefinitionException.class);
    exception
        .expectMessage(format("Extension '%s' has a connection provider named '%s' and an operation named operation with an argument type named equally.",
                              extensionModel.getName(), SIMPLE_PARAM_NAME));
    ParameterModel offending = getParameter(SIMPLE_PARAM_NAME, ChildTest.class);
    when(connectionProviderModel.getName()).thenReturn(SIMPLE_PARAM_NAME);
    when(operationModel.getParameterModels()).thenReturn(asList(topLevelOperationParam, offending));
    validate();
  }

  @Test
  public void connectionProviderNameClashesWithOperationParameterType() {
    exception.expect(IllegalModelDefinitionException.class);
    when(connectionProviderModel.getName()).thenReturn(SIMPLE_PARAM_NAME);
    ParameterModel offending = getParameter(connectionProviderModel.getName(), TopLevelTest.class);
    when(operationModel.getParameterModels()).thenReturn(asList(topLevelOperationParam, offending));
    validate();
  }

  @Test
  public void connectionProviderNameWithOperationParameterNoChild() {
    when(connectionProviderModel.getName()).thenReturn(SIMPLE_PARAM_NAME);
    ParameterModel offending = getParameter(connectionProviderModel.getName(), NoChildTest.class);
    when(operationModel.getParameterModels()).thenReturn(asList(topLevelOperationParam, offending));
    validate();
  }

  @Test
  public void configTopLevelParameterCrashesWithConfigName() {
    exception.expect(IllegalModelDefinitionException.class);
    when(configurationModel.getName()).thenReturn(CONFIG_NAME);
    ParameterModel offending = getParameter(CONFIG_NAME, TopLevelTest.class);
    when(configurationModel.getParameterModels()).thenReturn(asList(simpleConfigParam, topLevelConfigParam, offending));
    validate();
  }

  @Test
  public void connectionProviderTopLevelParameterCrashesWithConfigName() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(CONFIG_NAME, TopLevelTest.class);
    when(connectionProviderModel.getParameterModels())
        .thenReturn(asList(simpleConnectionProviderParam, topLevelConnectionProviderParam, offending));
    validate();
  }

  @Test
  public void operationTopLevelParameterCrashesWithOperationName() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(OPERATION_NAME, TopLevelTest.class);
    when(operationModel.getParameterModels()).thenReturn(asList(simpleOperationParam, topLevelOperationParam, offending));
    validate();
  }

  @Test
  public void connectionProviderTopLevelParameterCrashesWithName() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(CONNECTION_PROVIDER_NAME, TopLevelTest.class);
    when(connectionProviderModel.getParameterModels())
        .thenReturn(asList(simpleConnectionProviderParam, topLevelConnectionProviderParam, offending));
    validate();
  }

  @Test
  public void configWithRepeatedParameterName() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(SIMPLE_PARAM_NAME, String.class);
    when(configurationModel.getParameterModels()).thenReturn(asList(simpleConfigParam, topLevelConfigParam, offending));
    validate();
  }

  @Test
  public void operationWithRepeatedParameterName() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(SIMPLE_PARAM_NAME, String.class);
    when(operationModel.getParameterModels()).thenReturn(asList(simpleOperationParam, topLevelOperationParam, offending));
    validate();
  }

  @Test
  public void operationNameClashesWithParameterTypeName() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(TOP_LEVEL_SINGULAR_PARAM_NAME, TopLevelTest.class);
    when(operationModel.getName()).thenReturn(TopLevelTest.class.getName());
    when(operationModel.getParameterModels()).thenReturn(asList(topLevelOperationParam, offending));
    validate();
  }

  @Test
  public void connectionProviderWithRepeatedParameterName() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(SIMPLE_PARAM_NAME, String.class);
    when(connectionProviderModel.getParameterModels())
        .thenReturn(asList(simpleConnectionProviderParam, topLevelConnectionProviderParam, offending));
    validate();
  }

  @Test
  public void mapSingularizeClashOnOperation() {
    exception.expect(IllegalModelDefinitionException.class);
    when(operationModel.getName()).thenReturn(CHILD_SINGULAR_PARAM_NAME);
    ParameterModel offending = getParameter(CHILD_PLURAL_PARAM_NAME, childTestMap);
    when(operationModel.getParameterModels()).thenReturn(asList(offending));
    validate();
  }

  @Test
  public void mapSingularizeClashOnConnectionProvider() {
    exception.expect(IllegalModelDefinitionException.class);
    when(connectionProviderModel.getName()).thenReturn(CHILD_SINGULAR_PARAM_NAME);
    ParameterModel offending = getParameter(CHILD_PLURAL_PARAM_NAME, childTestMap);
    when(operationModel.getParameterModels()).thenReturn(asList(offending));
    validate();
  }

  @Test
  public void mapSingularizeClashOnParameterDifferentType() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(CHILD_PLURAL_PARAM_NAME, childTestMap);
    ParameterModel singular = getParameter(CHILD_SINGULAR_PARAM_NAME, String.class);
    when(operationModel.getParameterModels()).thenReturn(asList(singular, offending));
    validate();
  }

  @Test
  public void mapSingularizeClashOnParameterSameType() {
    ParameterModel offending = getParameter(CHILD_PLURAL_PARAM_NAME, childTestMap);
    ParameterModel singular = getParameter(CHILD_SINGULAR_PARAM_NAME, ChildTest.class);
    when(operationModel.getParameterModels()).thenReturn(asList(singular, offending));
    validate();
  }

  @Test
  public void mapSingularizeClashOnTopLevelParameterDifferentType() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(CHILD_PLURAL_PARAM_NAME, childTestMap);
    ParameterModel singular = getParameter(CHILD_SINGULAR_PARAM_NAME, TopLevelTest.class);
    when(operationModel.getParameterModels()).thenReturn(asList(singular, offending));
    validate();
  }

  @Test
  public void mapSingularizeClashOnTopLevelParameterSameType() {
    ParameterModel offending = getParameter(TOP_LEVEL_PLURAL_PARAM_NAME, topLevelMap);
    ParameterModel singular = getParameter(TOP_LEVEL_SINGULAR_PARAM_NAME, TopLevelTest.class);
    when(operationModel.getParameterModels()).thenReturn(asList(singular, offending));
    validate();
  }

  //////

  @Test
  public void listSingularizeClashOnOperation() {
    exception.expect(IllegalModelDefinitionException.class);
    when(operationModel.getName()).thenReturn(CHILD_SINGULAR_PARAM_NAME);
    ParameterModel offending = getParameter(CHILD_PLURAL_PARAM_NAME, childTestList);
    when(operationModel.getParameterModels()).thenReturn(asList(offending));
    validate();
  }

  @Test
  public void listSingularizeClashOnConnectionProvider() {
    exception.expect(IllegalModelDefinitionException.class);
    when(connectionProviderModel.getName()).thenReturn(CHILD_SINGULAR_PARAM_NAME);
    ParameterModel offending = getParameter(CHILD_PLURAL_PARAM_NAME, childTestList);
    when(operationModel.getParameterModels()).thenReturn(asList(offending));
    validate();
  }

  @Test
  public void listSingularizeClashOnParameterDifferentType() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(CHILD_PLURAL_PARAM_NAME, childTestList);
    ParameterModel singular = getParameter(CHILD_SINGULAR_PARAM_NAME, String.class);
    when(operationModel.getParameterModels()).thenReturn(asList(singular, offending));
    validate();
  }

  @Test
  public void listSingularizeClashOnParameterSameType() {
    ParameterModel offending = getParameter(CHILD_PLURAL_PARAM_NAME, childTestList);
    ParameterModel singular = getParameter(CHILD_SINGULAR_PARAM_NAME, ChildTest.class);
    when(operationModel.getParameterModels()).thenReturn(asList(singular, offending));
    validate();
  }

  @Test
  public void listSingularizeClashOnTopLevelParameterDifferentType() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(CHILD_PLURAL_PARAM_NAME, childTestList);
    ParameterModel singular = getParameter(CHILD_SINGULAR_PARAM_NAME, TopLevelTest.class);
    when(operationModel.getParameterModels()).thenReturn(asList(singular, offending));
    validate();
  }

  @Test
  public void listSingularizeClashOnTopLevelParameterSameType() {
    ParameterModel offending = getParameter(TOP_LEVEL_PLURAL_PARAM_NAME, topLevelTestList);
    ParameterModel singular = getParameter(TOP_LEVEL_SINGULAR_PARAM_NAME, TopLevelTest.class);
    when(operationModel.getParameterModels()).thenReturn(asList(singular, offending));
    validate();
  }

  @Test
  public void notInlineDefinitionPassSuccessful() {
    ParameterModel offending = getParameter(CHILD_PLURAL_PARAM_NAME, childTestList);
    ParameterModel singular = getParameter(CHILD_SINGULAR_PARAM_NAME, NoChildTest.class);
    when(configurationModel.getParameterModels()).thenReturn(singletonList(offending));
    when(operationModel.getName()).thenReturn("mapSingularizeClash");
    when(operationModel.getParameterModels()).thenReturn(singletonList(singular));
    validate();
  }

  @Test
  public void differentNamesClashWhenHyphenized() {
    exception.expect(IllegalModelDefinitionException.class);
    exception.expectMessage("contains 2 components 'config-name");
    ConfigurationModel configuration = mock(ConfigurationModel.class);
    mockModelProperties(configuration);
    when(configuration.getName()).thenReturn("config-name");
    when(configurationModel.getName()).thenReturn("ConfigName");
    when(configuration.getParameterModels()).thenReturn(asList(simpleConfigParam, topLevelConfigParam));
    when(configuration.getOperationModels()).thenReturn(ImmutableList.of());
    when(configuration.getConnectionProviders()).thenReturn(ImmutableList.of());
    when(extensionModel.getConfigurationModels()).thenReturn(asList(configurationModel, configuration));
    validate();
  }

  private void validate() {
    validator.validate(extensionModel);
  }

  @XmlHints(allowTopLevelDefinition = true, allowInlineDefinition = true)
  private static class TopLevelTest {

    public TopLevelTest() {

    }

    private String id;

    public String getId() {
      return id;
    }
  }

  @XmlHints(allowTopLevelDefinition = false, allowInlineDefinition = true)
  private static class ChildTest {

    public ChildTest() {

    }

    private String id;

    public String getId() {
      return id;
    }
  }

  @XmlHints(allowInlineDefinition = false)
  private static class NoChildTest {

    private String id;

    public String getId() {
      return id;
    }
  }
}
