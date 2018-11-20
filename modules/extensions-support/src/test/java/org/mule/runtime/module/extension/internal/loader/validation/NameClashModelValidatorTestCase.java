/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.CONTENT;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.PRIMARY_CONTENT;
import static org.mule.runtime.api.util.ExtensionModelTestUtils.visitableMock;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.SOURCE;
import static org.mule.runtime.extension.api.util.NameUtils.hyphenize;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.getParameter;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockParameters;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.SubTypesModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.function.FunctionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceCallbackModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.model.source.ImmutableSourceModel;
import org.mule.runtime.extension.internal.loader.validator.NameClashModelValidator;
import org.mule.runtime.extension.internal.property.PagedOperationModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ConfigTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectivityModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.test.module.extension.internal.util.ExtensionsTestUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.HashMap;
import java.util.List;

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

  private static final String FUNCTION_NAME = "function";
  private static final String CONSTRUCT_NAME = "construct";
  private static final String TOP_LEVEL_CONSTRUCT_PARAM_NAME = "topLevelConstructParam";
  private static final String OPERATION_NAME = "operation";
  private static final String TOP_LEVEL_OPERATION_PARAM_NAME = "topLevelOperationParam";
  private static final String CONFIG_NAME = "config";
  private static final String CONFIG_SUFFIX = "Config";
  private static final String SOURCE_NAME = "source";
  private static final String CONNECTION_PROVIDER_NAME = "connectionProviderConnection";
  private static final String CONNECTION_PROVIDER_SUFFIX = "Connection";
  private static final String SIMPLE_PARAM_NAME = "simple";

  private static final String TOP_LEVEL_PLURAL_PARAM_NAME = "topLevelTests";
  private static final String TOP_LEVEL_SINGULAR_PARAM_NAME = "topLevelTest";

  private static final String CHILD_PLURAL_PARAM_NAME = "childTests";
  private static final String CHILD_SINGULAR_PARAM_NAME = "childTest";

  private static final String REPEATED_NAME = "repeatedName";
  private static final String UNIQUE_PARAM_NAME = "uniqueParam";

  @Mock
  private ExtensionModel extensionModel;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ConfigurationModel configurationModel;

  @Mock
  private OperationModel operationModel;

  @Mock
  private ConnectionProviderModel connectionProviderModel;

  @Mock
  private SourceModel sourceModel;

  @Mock
  private ConstructModel constructModel;

  @Mock
  private FunctionModel functionModel;

  private XmlDslModel xmlDslModel = new XmlDslModel();
  private ParameterModel simpleConfigParam;
  private ParameterModel topLevelConfigParam;
  private ParameterModel simpleOperationParam;
  private ParameterModel topLevelOperationParam;
  private ParameterModel simpleConnectionProviderParam;
  private ParameterModel topLevelConnectionProviderParam;
  private ParameterModel simpleConstructParam;
  private ParameterModel topLevelConstructParam;
  private ParameterModel simpleFunctionParam;
  private BaseTypeBuilder baseTypeBuilder = BaseTypeBuilder.create(JAVA);

  private MetadataType childTestList = baseTypeBuilder.arrayType()
      .of(toMetadataType(ChildTest.class)).build();

  private MetadataType topLevelTestList = baseTypeBuilder.arrayType()
      .of(toMetadataType(TopLevelTest.class)).build();

  private MetadataType childTestMap = baseTypeBuilder.objectType()
      .openWith(toMetadataType(ChildTest.class))
      .with(new ClassInformationAnnotation(HashMap.class, asList(String.class, ChildTest.class)))
      .build();

  private MetadataType topLevelMap = baseTypeBuilder.objectType()
      .openWith(toMetadataType(TopLevelTest.class))
      .with(new ClassInformationAnnotation(HashMap.class, asList(String.class, TopLevelTest.class)))
      .build();

  private NameClashModelValidator validator = new NameClashModelValidator();

  @Before
  public void before() {
    when(extensionModel.getName()).thenReturn("extensionName");
    when(extensionModel.getConfigurationModels()).thenReturn(singletonList(configurationModel));
    when(extensionModel.getOperationModels()).thenReturn(singletonList(operationModel));
    when(extensionModel.getConnectionProviders()).thenReturn(singletonList(connectionProviderModel));
    when(extensionModel.getSourceModels()).thenReturn(singletonList(sourceModel));
    when(extensionModel.getConstructModels()).thenReturn(singletonList(constructModel));
    when(extensionModel.getXmlDslModel()).thenReturn(xmlDslModel);
    when(extensionModel.getFunctionModels()).thenReturn(singletonList(functionModel));

    simpleConfigParam = getParameter(SIMPLE_PARAM_NAME, String.class);
    topLevelConfigParam = getParameter(TOP_LEVEL_OPERATION_PARAM_NAME, TopLevelTest.class);

    simpleOperationParam = getParameter(SIMPLE_PARAM_NAME, String.class);
    topLevelOperationParam = getParameter(TOP_LEVEL_OPERATION_PARAM_NAME, TopLevelTest.class);

    simpleConnectionProviderParam = getParameter(SIMPLE_PARAM_NAME, String.class);
    topLevelConnectionProviderParam = getParameter(TOP_LEVEL_OPERATION_PARAM_NAME, TopLevelTest.class);

    simpleConstructParam = getParameter(SIMPLE_PARAM_NAME, String.class);
    topLevelConstructParam = getParameter(TOP_LEVEL_CONSTRUCT_PARAM_NAME, TopLevelTest.class);

    simpleFunctionParam = getParameter(SIMPLE_PARAM_NAME, String.class);

    when(configurationModel.getName()).thenReturn(CONFIG_NAME);
    when(configurationModel.getAllParameterModels()).thenReturn(asList(simpleConfigParam, topLevelConfigParam));
    when(configurationModel.getOperationModels()).thenReturn(ImmutableList.of());
    when(configurationModel.getConnectionProviders()).thenReturn(ImmutableList.of());
    when(configurationModel.getParameterGroupModels()).thenReturn(ImmutableList.of());

    when(operationModel.getName()).thenReturn(OPERATION_NAME);
    when(operationModel.getAllParameterModels()).thenReturn(asList(simpleOperationParam, topLevelOperationParam));

    when(constructModel.getName()).thenReturn(CONSTRUCT_NAME);
    when(constructModel.getAllParameterModels()).thenReturn(asList(simpleConstructParam, topLevelConstructParam));

    when(functionModel.getName()).thenReturn(FUNCTION_NAME);
    when(functionModel.getAllParameterModels()).thenReturn(asList(simpleFunctionParam));

    mockModelProperties(operationModel);
    mockModelProperties(configurationModel);
    mockModelProperties(constructModel);
    mockModelProperties(connectionProviderModel);
    when(connectionProviderModel.getName()).thenReturn(CONNECTION_PROVIDER_NAME);
    when(connectionProviderModel.getAllParameterModels())
        .thenReturn(asList(simpleConnectionProviderParam, topLevelConnectionProviderParam));

    when(sourceModel.getName()).thenReturn(SOURCE_NAME);
    when(sourceModel.getModelProperty(any())).thenReturn(empty());
    when(sourceModel.getErrorCallback()).thenReturn(empty());
    when(sourceModel.getSuccessCallback()).thenReturn(empty());

    visitableMock(operationModel);
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
    String clashingName = OPERATION_NAME + CONFIG_SUFFIX;
    when(operationModel.getName()).thenReturn(clashingName);
    exception.expect(IllegalModelDefinitionException.class);
    when(configurationModel.getName()).thenReturn(clashingName);
    validate();
  }

  @Test
  public void configParameterWithRepeatedName() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(TOP_LEVEL_OPERATION_PARAM_NAME, Banana.class);
    mockParameters(configurationModel, simpleConfigParam, topLevelConfigParam, offending);
    validate();
  }

  @Test
  public void configNameClashesWithOperationParameterType() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(SIMPLE_PARAM_NAME, TopLevelConfig.class);
    when(configurationModel.getName()).thenReturn(TopLevelConfig.class.getName());
    mockParameters(operationModel, topLevelConfigParam, offending);
    validate();
  }

  @Test
  public void connectionProviderTopLevelParameterCrashesWithOperationName() {
    exception.expect(IllegalModelDefinitionException.class);
    exception.expectMessage("contains 2 components 'operation' which it's transformed DSL name is 'operation'.");
    ParameterModel offending = getParameter(OPERATION_NAME, TopLevelTest.class);
    mockParameters(connectionProviderModel, simpleConnectionProviderParam, topLevelConnectionProviderParam, offending);
    validate();
  }

  @Test
  public void connectionProviderNameClashesWithOperationParameterName() {
    String clashingName = SIMPLE_PARAM_NAME + CONNECTION_PROVIDER_SUFFIX;
    exception.expect(IllegalModelDefinitionException.class);
    exception.expectMessage(format(
                                   "Extension '%s' has a connection provider named '%s' and an operation named operation with an argument type named equally.",
                                   extensionModel.getName(), hyphenize(clashingName)));
    ParameterModel offending = getParameter(clashingName, ChildTest.class);
    when(connectionProviderModel.getName()).thenReturn(clashingName);
    when(operationModel.getAllParameterModels()).thenReturn(asList(topLevelOperationParam, offending));
    validate();
  }

  @Test
  public void connectionProviderNameClashesWithOperationParameterType() {
    exception.expect(IllegalModelDefinitionException.class);
    String clashingName = SIMPLE_PARAM_NAME + CONNECTION_PROVIDER_SUFFIX;
    when(connectionProviderModel.getName()).thenReturn(clashingName);
    ParameterModel offending = getParameter(clashingName, TopLevelTest.class);
    when(operationModel.getAllParameterModels()).thenReturn(asList(topLevelOperationParam, offending));
    validate();
  }

  @Test
  public void connectionProviderNameWithOperationParameterNoChild() {
    when(connectionProviderModel.getName()).thenReturn(SIMPLE_PARAM_NAME);
    ParameterModel offending = getParameter(connectionProviderModel.getName(), NoChildTest.class);
    when(operationModel.getAllParameterModels()).thenReturn(asList(topLevelOperationParam, offending));
    validate();
  }

  @Test
  public void configTopLevelParameterCrashesWithConfigName() {
    exception.expect(IllegalModelDefinitionException.class);
    when(configurationModel.getName()).thenReturn(CONFIG_NAME);
    ParameterModel offending = getParameter(CONFIG_NAME, TopLevelTest.class);
    mockParameters(configurationModel, simpleConfigParam, topLevelConfigParam, offending);
    validate();
  }

  @Test
  public void connectionProviderTopLevelParameterCrashesWithConfigName() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(CONFIG_NAME, TopLevelTest.class);
    mockParameters(connectionProviderModel, simpleConnectionProviderParam, topLevelConnectionProviderParam, offending);
    validate();
  }

  @Test
  public void operationTopLevelParameterCrashesWithOperationName() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(OPERATION_NAME, TopLevelTest.class);
    when(operationModel.getAllParameterModels()).thenReturn(asList(simpleOperationParam, topLevelOperationParam, offending));
    validate();
  }

  @Test
  public void connectionProviderTopLevelParameterCrashesWithName() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(CONNECTION_PROVIDER_NAME, TopLevelTest.class);
    mockParameters(connectionProviderModel, simpleConnectionProviderParam, topLevelConnectionProviderParam, offending);
    validate();
  }

  @Test
  public void configWithRepeatedParameterName() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(SIMPLE_PARAM_NAME, String.class);
    mockParameters(configurationModel, simpleConfigParam, topLevelConfigParam, offending);
    validate();
  }

  @Test
  public void operationWithRepeatedParameterName() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(SIMPLE_PARAM_NAME, String.class);
    mockParameters(operationModel, simpleConfigParam, topLevelConfigParam, offending);
    validate();
  }

  @Test
  public void operationNameClashesWithParameterTypeName() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(TOP_LEVEL_SINGULAR_PARAM_NAME, TopLevelTest.class);
    when(operationModel.getName()).thenReturn(TopLevelTest.class.getSimpleName());
    mockParameters(operationModel, topLevelOperationParam, offending);
    validate();
  }

  @Test
  public void connectionProviderWithRepeatedParameterName() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(SIMPLE_PARAM_NAME, String.class);
    mockParameters(connectionProviderModel, simpleConfigParam, topLevelConfigParam, offending);
    validate();
  }

  @Test
  public void contentParameterValidationIsSkipped() {
    ParameterModel offending = getParameter(SIMPLE_PARAM_NAME, String.class);
    when(offending.getRole()).thenReturn(CONTENT);
    when(connectionProviderModel.getAllParameterModels())
        .thenReturn(asList(simpleConnectionProviderParam, topLevelConnectionProviderParam, offending));
    validate();
  }

  @Test
  public void sourceWithRepeatedParameterNameWithinCallback() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(SIMPLE_PARAM_NAME, String.class);
    SourceCallbackModel sourceCallbackModel = mock(SourceCallbackModel.class);
    mockParameters(sourceCallbackModel, simpleConnectionProviderParam, offending);
    when(sourceModel.getErrorCallback()).thenReturn(of(sourceCallbackModel));
    validate();
  }

  @Test
  public void sourceWithRepeatedParameterNameAmongCallbackAndSource() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(SIMPLE_PARAM_NAME, String.class);
    SourceCallbackModel sourceCallbackModel = mock(SourceCallbackModel.class);
    mockParameters(sourceCallbackModel, simpleConfigParam);

    ParameterGroupModel group = mock(ParameterGroupModel.class);
    when(group.getName()).thenReturn(DEFAULT_GROUP_NAME);
    when(group.getModelProperty(ParameterGroupModelProperty.class)).thenReturn(empty());
    when(group.isShowInDsl()).thenReturn(false);
    when(group.getParameterModels()).thenReturn(asList(offending));

    SourceModel sourceModel = new ImmutableSourceModel(SOURCE_NAME, "", false, false, asList(group), emptyList(), null, null,
                                                       of(sourceCallbackModel), empty(), empty(), false, false, false,
                                                       null, SOURCE, emptySet(), emptySet(), emptySet(), null);
    when(extensionModel.getSourceModels()).thenReturn(asList(sourceModel));
    validate();
  }

  @Test
  public void sourceWithRepeatedParameterNameAmongCallbacks() {
    SourceCallbackModel errorCallBack = mock(SourceCallbackModel.class);
    when(errorCallBack.getAllParameterModels()).thenReturn(asList(simpleConnectionProviderParam));
    when(sourceModel.getErrorCallback()).thenReturn(of(errorCallBack));

    SourceCallbackModel successCallback = mock(SourceCallbackModel.class);
    when(successCallback.getAllParameterModels()).thenReturn(asList(simpleConnectionProviderParam));
    when(sourceModel.getSuccessCallback()).thenReturn(of(successCallback));
    validate();
  }

  @Test
  public void mapSingularizeClashOnOperation() {
    exception.expect(IllegalModelDefinitionException.class);
    when(operationModel.getName()).thenReturn(CHILD_SINGULAR_PARAM_NAME);
    ParameterModel offending = getParameter(CHILD_PLURAL_PARAM_NAME, childTestMap);
    when(operationModel.getAllParameterModels()).thenReturn(asList(offending));
    validate();
  }

  @Test
  public void mapSingularizeClashOnConnectionProvider() {
    exception.expect(IllegalModelDefinitionException.class);
    String clashingName = CHILD_PLURAL_PARAM_NAME + CONNECTION_PROVIDER_SUFFIX;
    when(connectionProviderModel.getName()).thenReturn(clashingName);
    ParameterModel offending = getParameter(clashingName, childTestMap);
    when(operationModel.getAllParameterModels()).thenReturn(asList(offending));
    validate();
  }

  @Test
  public void mapSingularizeClashOnParameterSameType() {
    ParameterModel offending = getParameter(CHILD_PLURAL_PARAM_NAME, childTestMap);
    ParameterModel singular = getParameter(CHILD_SINGULAR_PARAM_NAME, ChildTest.class);
    when(operationModel.getAllParameterModels()).thenReturn(asList(singular, offending));
    validate();
  }

  @Test
  public void mapSingularizeClashOnTopLevelParameterDifferentType() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(CHILD_PLURAL_PARAM_NAME, childTestMap);
    ParameterModel singular = getParameter(CHILD_SINGULAR_PARAM_NAME, TopLevelTest.class);
    when(operationModel.getAllParameterModels()).thenReturn(asList(singular, offending));
    validate();
  }

  @Test
  public void mapSingularizeClashOnTopLevelParameterSameType() {
    ParameterModel offending = getParameter(TOP_LEVEL_PLURAL_PARAM_NAME, topLevelMap);
    ParameterModel singular = getParameter(TOP_LEVEL_SINGULAR_PARAM_NAME, TopLevelTest.class);
    when(operationModel.getAllParameterModels()).thenReturn(asList(singular, offending));
    validate();
  }

  @Test
  public void listSingularizeClashOnOperation() {
    exception.expect(IllegalModelDefinitionException.class);
    when(operationModel.getName()).thenReturn(CHILD_SINGULAR_PARAM_NAME);
    ParameterModel offending = getParameter(CHILD_PLURAL_PARAM_NAME, childTestList);
    when(operationModel.getAllParameterModels()).thenReturn(asList(offending));
    validate();
  }

  @Test
  public void listSingularizeClashOnConnectionProvider() {
    String clashingName = CHILD_PLURAL_PARAM_NAME + CONNECTION_PROVIDER_SUFFIX;
    exception.expect(IllegalModelDefinitionException.class);
    when(connectionProviderModel.getName()).thenReturn(clashingName);
    ParameterModel offending = getParameter(clashingName, childTestList);
    when(operationModel.getAllParameterModels()).thenReturn(asList(offending));
    validate();
  }

  @Test
  public void listSingularizeClashOnParameterSameType() {
    ParameterModel offending = getParameter(CHILD_PLURAL_PARAM_NAME, childTestList);
    ParameterModel singular = getParameter(CHILD_SINGULAR_PARAM_NAME, ChildTest.class);
    when(operationModel.getAllParameterModels()).thenReturn(asList(singular, offending));
    validate();
  }

  @Test
  public void listSingularizeClashOnTopLevelParameterDifferentType() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(CHILD_PLURAL_PARAM_NAME, childTestList);
    ParameterModel singular = getParameter(CHILD_SINGULAR_PARAM_NAME, TopLevelTest.class);
    when(operationModel.getAllParameterModels()).thenReturn(asList(singular, offending));
    validate();
  }

  @Test
  public void listSingularizeClashOnTopLevelParameterSameType() {
    ParameterModel offending = getParameter(TOP_LEVEL_PLURAL_PARAM_NAME, topLevelTestList);
    ParameterModel singular = getParameter(TOP_LEVEL_SINGULAR_PARAM_NAME, TopLevelTest.class);
    when(operationModel.getAllParameterModels()).thenReturn(asList(singular, offending));
    validate();
  }

  @Test
  public void topLevelParameterClashOnSubtypeWithSameType() {
    ParameterModel offending = getParameter(TOP_LEVEL_SINGULAR_PARAM_NAME, TopLevelTest.class);
    mockParameters(operationModel, offending);
    when(extensionModel.getSubTypes()).thenReturn(ImmutableSet
        .of(new SubTypesModel(toMetadataType(ChildTest.class), ImmutableSet.of(toMetadataType(TopLevelTest.class)))));
    validate();
  }

  @Test
  public void topLevelParameterClashOnSubtypeWithDifferentType() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(TOP_LEVEL_SINGULAR_PARAM_NAME, TopLevelTest.class);
    mockParameters(operationModel, offending);
    when(extensionModel.getSubTypes()).thenReturn(ImmutableSet
        .of(new SubTypesModel(toMetadataType(ChildTest.class), ImmutableSet.of(toMetadataType(TopLevelSubtype.class)))));
    validate();
  }

  @Test
  public void notInlineDefinitionPassSuccessful() {
    ParameterModel offending = getParameter(CHILD_PLURAL_PARAM_NAME, childTestList);
    ParameterModel singular = getParameter(CHILD_SINGULAR_PARAM_NAME, NoChildTest.class);
    when(configurationModel.getAllParameterModels()).thenReturn(singletonList(offending));
    when(operationModel.getName()).thenReturn("mapSingularizeClash");
    when(operationModel.getAllParameterModels()).thenReturn(singletonList(singular));
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
    when(configuration.getAllParameterModels()).thenReturn(asList(simpleConfigParam, topLevelConfigParam));
    when(configuration.getOperationModels()).thenReturn(ImmutableList.of());
    when(configuration.getConnectionProviders()).thenReturn(ImmutableList.of());
    when(extensionModel.getConfigurationModels()).thenReturn(asList(configurationModel, configuration));
    validate();
  }

  @Test
  public void contentParametersWithSameNameAndDifferentType() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel firstParam = getParameter(CHILD_SINGULAR_PARAM_NAME, Object.class);
    when(firstParam.getRole()).thenReturn(PRIMARY_CONTENT);
    ParameterModel secondParam = getParameter(CHILD_SINGULAR_PARAM_NAME, TopLevelTest.class);
    when(secondParam.getRole()).thenReturn(CONTENT);
    when(operationModel.getAllParameterModels()).thenReturn(asList(firstParam));
    when(sourceModel.getAllParameterModels()).thenReturn(asList(secondParam));
    validate();
  }

  @Test
  public void contentParametersWithSameNameAndTypeButDifferentRole() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel firstParam = getParameter(CHILD_SINGULAR_PARAM_NAME, TopLevelTest.class);
    when(firstParam.getRole()).thenReturn(PRIMARY_CONTENT);
    ParameterModel secondParam = getParameter(CHILD_SINGULAR_PARAM_NAME, TopLevelTest.class);
    when(secondParam.getRole()).thenReturn(BEHAVIOUR);
    when(operationModel.getAllParameterModels()).thenReturn(asList(firstParam));
    when(sourceModel.getAllParameterModels()).thenReturn(asList(secondParam));
    validate();
  }

  @Test
  public void contentParameterClashWithAttributeParameterWithinSameGroup() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterGroupModel group = mock(ParameterGroupModel.class);
    ParameterModel contentParam = getParameter(CHILD_SINGULAR_PARAM_NAME, Object.class);
    when(contentParam.getRole()).thenReturn(PRIMARY_CONTENT);

    ParameterModel notContentParam = getParameter(CHILD_SINGULAR_PARAM_NAME, String.class);
    when(notContentParam.getRole()).thenReturn(BEHAVIOUR);

    when(group.getName()).thenReturn(DEFAULT_GROUP_NAME);
    when(group.getModelProperty(ParameterGroupModelProperty.class)).thenReturn(empty());
    when(group.getParameterModels()).thenReturn(asList(contentParam));

    ParameterGroupModel anotherGroup = mock(ParameterGroupModel.class);
    when(anotherGroup.getName()).thenReturn("My Group");
    when(anotherGroup.isShowInDsl()).thenReturn(false);
    when(anotherGroup.getParameterModels()).thenReturn(asList(notContentParam));

    when(operationModel.getParameterGroupModels()).thenReturn(asList(group, anotherGroup));
    validate();
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void invalidModelDueToRepeatedParameterWithGroupThatDoesntShowInDsl() {
    mockParameterGroups(operationModel, false);
    validate();
  }

  @Test
  public void repeatedParameterWithGroupThatShowsInDsl() {
    mockParameterGroups(operationModel, true);
    validate();
  }

  @Test
  public void repeatedContentParameterNameAndConfiguration() {
    exception.expect(IllegalModelDefinitionException.class);
    when(configurationModel.getName()).thenReturn(REPEATED_NAME);
    ParameterModel param = getParameter(REPEATED_NAME + CONFIG_SUFFIX, ChildObjectTest.class);
    when(param.getRole()).thenReturn(PRIMARY_CONTENT);
    when(operationModel.getAllParameterModels()).thenReturn(asList(param));
    validate();
  }

  @Test
  public void repeatedContentParameterNameAndOperation() {
    exception.expect(IllegalModelDefinitionException.class);
    when(operationModel.getName()).thenReturn(REPEATED_NAME);
    ParameterModel param = getParameter(REPEATED_NAME, ChildObjectTest.class);
    when(param.getRole()).thenReturn(PRIMARY_CONTENT);
    when(operationModel.getAllParameterModels()).thenReturn(asList(param));
    validate();
  }

  @Test
  public void repeatedContentParameterNameAndSource() {
    exception.expect(IllegalModelDefinitionException.class);
    when(sourceModel.getName()).thenReturn(REPEATED_NAME);
    ParameterModel param = getParameter(REPEATED_NAME, ChildObjectTest.class);
    when(param.getRole()).thenReturn(PRIMARY_CONTENT);
    when(operationModel.getAllParameterModels()).thenReturn(asList(param));
    validate();
  }

  @Test
  public void repeatedContentParameterNameAndChildElementSupport() {
    exception.expect(IllegalModelDefinitionException.class);

    OperationModel anotherOperationModel = mock(OperationModel.class);

    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel, anotherOperationModel));
    when(anotherOperationModel.getName()).thenReturn("anotherOperation");

    ParameterModel param = getParameter(REPEATED_NAME, String.class);
    ParameterModel anotherParam = getParameter(UNIQUE_PARAM_NAME, ChildElementTest.class);
    when(param.getRole()).thenReturn(PRIMARY_CONTENT);
    when(anotherParam.getRole()).thenReturn(BEHAVIOUR);
    when(operationModel.getAllParameterModels()).thenReturn(asList(param));
    when(anotherOperationModel.getAllParameterModels()).thenReturn(asList(anotherParam));

    mockParameterGroup(operationModel, asList(param));
    mockParameterGroup(anotherOperationModel, asList(anotherParam));

    validate();
  }

  @Test
  public void repeatedChildElementSupportNameAndChildElementSupportDifferentType() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel firstParam = getParameter(UNIQUE_PARAM_NAME + "1", ChildElementTest.class);
    ParameterModel secondParam = getParameter(UNIQUE_PARAM_NAME + "2", ChildElementTestClone.class);
    when(firstParam.getRole()).thenReturn(BEHAVIOUR);
    when(secondParam.getRole()).thenReturn(BEHAVIOUR);
    when(operationModel.getAllParameterModels()).thenReturn(asList(firstParam, secondParam));

    mockParameterGroup(operationModel, asList(firstParam, secondParam));

    validate();
  }

  @Test
  public void repeatedChildElementSupportNameAndChildElementSupportSameType() {
    ParameterModel firstParam = getParameter(UNIQUE_PARAM_NAME + "1", ChildElementTest.class);
    ParameterModel secondParam = getParameter(UNIQUE_PARAM_NAME + "2", ChildElementTest.class);
    when(firstParam.getRole()).thenReturn(BEHAVIOUR);
    when(secondParam.getRole()).thenReturn(BEHAVIOUR);
    when(operationModel.getAllParameterModels()).thenReturn(asList(firstParam, secondParam));

    mockParameterGroup(operationModel, asList(firstParam, secondParam));

    validate();
  }

  @Test
  public void repeatedChildElementSupportNameAndChildElementSupportSameTypeDifferentOperation() {
    OperationModel anotherOperationModel = mock(OperationModel.class);

    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel, anotherOperationModel));
    when(anotherOperationModel.getName()).thenReturn("anotherOperation");

    ParameterModel param = getParameter(UNIQUE_PARAM_NAME + "1", ChildElementTest.class);
    ParameterModel anotherParam = getParameter(UNIQUE_PARAM_NAME + "2", ChildElementTest.class);
    when(param.getRole()).thenReturn(BEHAVIOUR);
    when(anotherParam.getRole()).thenReturn(BEHAVIOUR);
    when(operationModel.getAllParameterModels()).thenReturn(asList(param));
    when(anotherOperationModel.getAllParameterModels()).thenReturn(asList(anotherParam));

    mockParameterGroup(operationModel, asList(param));
    mockParameterGroup(anotherOperationModel, asList(anotherParam));

    validate();
  }

  @Test
  public void repeatedChildElementSupportNameAndChildElementSupportDifferenctTypeDifferentOperation() {
    exception.expect(IllegalModelDefinitionException.class);
    OperationModel anotherOperationModel = mock(OperationModel.class);

    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel, anotherOperationModel));
    when(anotherOperationModel.getName()).thenReturn("anotherOperation");

    ParameterModel param = getParameter(UNIQUE_PARAM_NAME + "1", ChildElementTest.class);
    ParameterModel anotherParam = getParameter(UNIQUE_PARAM_NAME + "2", ChildElementTestClone.class);
    when(param.getRole()).thenReturn(BEHAVIOUR);
    when(anotherParam.getRole()).thenReturn(BEHAVIOUR);
    when(operationModel.getAllParameterModels()).thenReturn(asList(param));
    when(anotherOperationModel.getAllParameterModels()).thenReturn(asList(anotherParam));

    mockParameterGroup(operationModel, asList(param));
    mockParameterGroup(anotherOperationModel, asList(anotherParam));

    validate();
  }

  @Test
  public void repeatedChildElementSupportAndOperation() {
    exception.expect(IllegalModelDefinitionException.class);
    when(operationModel.getName()).thenReturn(REPEATED_NAME);
    when(operationModel.getAllParameterModels()).thenReturn(emptyList());
    ParameterModel param = getParameter(UNIQUE_PARAM_NAME, ChildElementTest.class);
    when(param.getRole()).thenReturn(BEHAVIOUR);
    when(constructModel.getAllParameterModels()).thenReturn(asList(param));

    mockParameterGroup(constructModel, asList(param));

    validate();
  }

  @Test
  public void repeatedChildElementSupportNameAndSource() {
    exception.expect(IllegalModelDefinitionException.class);
    when(sourceModel.getName()).thenReturn(REPEATED_NAME);

    ParameterModel param = getParameter(UNIQUE_PARAM_NAME, ChildElementTest.class);
    when(param.getRole()).thenReturn(BEHAVIOUR);
    when(operationModel.getAllParameterModels()).thenReturn(asList(param));

    mockParameterGroup(operationModel, asList(param));

    validate();
  }

  @Test
  public void listSingularizeClashOnConstruct() {
    exception.expect(IllegalModelDefinitionException.class);
    when(constructModel.getName()).thenReturn(CHILD_SINGULAR_PARAM_NAME);
    ParameterModel offending = getParameter(CHILD_PLURAL_PARAM_NAME, childTestList);
    when(constructModel.getAllParameterModels()).thenReturn(asList(offending));
    validate();
  }

  @Test
  public void mapSingularizeClashOnConstruct() {
    exception.expect(IllegalModelDefinitionException.class);
    when(constructModel.getName()).thenReturn(CHILD_SINGULAR_PARAM_NAME);
    ParameterModel offending = getParameter(CHILD_PLURAL_PARAM_NAME, childTestMap);
    when(constructModel.getAllParameterModels()).thenReturn(asList(offending));
    validate();
  }

  @Test
  public void listSingularizeClashOnParameterSameTypeOnConstruct() {
    ParameterModel offending = getParameter(CHILD_PLURAL_PARAM_NAME, childTestList);
    ParameterModel singular = getParameter(CHILD_SINGULAR_PARAM_NAME, ChildTest.class);
    when(constructModel.getAllParameterModels()).thenReturn(asList(singular, offending));
    validate();
  }

  @Test
  public void listSingularizeClashOnTopLevelParameterDifferentTypeOnConstruct() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(CHILD_PLURAL_PARAM_NAME, childTestList);
    ParameterModel singular = getParameter(CHILD_SINGULAR_PARAM_NAME, ChildObjectTest.class);
    when(constructModel.getAllParameterModels()).thenReturn(asList(singular, offending));
    validate();
  }

  @Test
  public void repeatedFunctionNames() {
    exception.expect(IllegalModelDefinitionException.class);
    FunctionModel anotherFunctionModel = mock(FunctionModel.class);
    when(anotherFunctionModel.getName()).thenReturn(FUNCTION_NAME);
    when(anotherFunctionModel.getAllParameterModels()).thenReturn(emptyList());
    when(extensionModel.getFunctionModels()).thenReturn(asList(functionModel, anotherFunctionModel));
    validate();
  }

  @Test
  public void differentFunctionNames() {
    FunctionModel anotherFunctionModel = mock(FunctionModel.class);
    when(anotherFunctionModel.getName()).thenReturn("another-" + FUNCTION_NAME);
    when(anotherFunctionModel.getAllParameterModels()).thenReturn(emptyList());
    when(extensionModel.getFunctionModels()).thenReturn(asList(functionModel, anotherFunctionModel));
    validate();
  }

  @Test
  public void functionNameClashWithOperationName() {
    when(functionModel.getName()).thenReturn(OPERATION_NAME);
    validate();
  }

  @Test
  public void operationNameClashesWithAnotherOperationName() {
    exception.expect(IllegalModelDefinitionException.class);
    OperationModel anotherOperationModel = mock(OperationModel.class);
    when(anotherOperationModel.getName()).thenReturn(OPERATION_NAME);
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel, anotherOperationModel));
    validate();
  }

  @Test
  public void configurationNameClashesWithAnotherConfigurationName() {
    exception.expect(IllegalModelDefinitionException.class);
    ConfigurationModel anotherConfigurationModel = mock(ConfigurationModel.class);
    when(anotherConfigurationModel.getName()).thenReturn(CONFIG_NAME);
    when(extensionModel.getConfigurationModels()).thenReturn(asList(configurationModel, anotherConfigurationModel));
    validate();
  }

  @Test
  public void sourceNameClashesWithSourceName() {
    exception.expect(IllegalModelDefinitionException.class);
    SourceModel anotherSourceModel = mock(SourceModel.class);
    when(anotherSourceModel.getName()).thenReturn(SOURCE_NAME);
    when(anotherSourceModel.getErrorCallback()).thenReturn(empty());
    when(anotherSourceModel.getSuccessCallback()).thenReturn(empty());
    when(extensionModel.getSourceModels()).thenReturn(asList(sourceModel, anotherSourceModel));
    validate();
  }

  @Test
  public void constructNameClashesWithAnotherConstructName() {
    exception.expect(IllegalModelDefinitionException.class);
    ConstructModel anotherConstructModel = mock(ConstructModel.class);
    when(anotherConstructModel.getName()).thenReturn(CONSTRUCT_NAME);
    when(extensionModel.getConstructModels()).thenReturn(asList(constructModel, anotherConstructModel));
    validate();
  }

  @Test
  public void connectionProviderNameClashesWithAnotherConnectionProviderName() {
    exception.expect(IllegalModelDefinitionException.class);
    ConnectionProviderModel anotherConnectionProviderModel = mock(ConnectionProviderModel.class);
    when(anotherConnectionProviderModel.getName()).thenReturn(CONNECTION_PROVIDER_NAME);
    when(extensionModel.getConnectionProviders()).thenReturn(asList(connectionProviderModel, anotherConnectionProviderModel));
    validate();
  }

  @Test
  public void functionParameterClashesWithOperationParameter() {
    ParameterModel param = getParameter(UNIQUE_PARAM_NAME + "1", ChildElementTest.class);
    ParameterModel anotherParam = getParameter(UNIQUE_PARAM_NAME + "2", ChildElementTestClone.class);
    when(param.getRole()).thenReturn(BEHAVIOUR);
    when(anotherParam.getRole()).thenReturn(BEHAVIOUR);
    when(operationModel.getAllParameterModels()).thenReturn(asList(param));
    when(functionModel.getAllParameterModels()).thenReturn(asList(anotherParam));

    mockParameterGroup(operationModel, asList(param));
    mockParameterGroup(functionModel, asList(anotherParam));

    validate();
  }

  @Test
  public void functionParameterClashesWithOperationName() {
    when(operationModel.getName()).thenReturn(REPEATED_NAME);
    ParameterModel param = getParameter(UNIQUE_PARAM_NAME, ChildElementTest.class);
    when(param.getRole()).thenReturn(BEHAVIOUR);
    when(functionModel.getAllParameterModels()).thenReturn(asList(param));
    mockParameterGroup(functionModel, asList(param));

    validate();
  }

  private void mockParameterGroup(ParameterizedModel model, List<ParameterModel> parameters) {
    ParameterGroupModel group = mock(ParameterGroupModel.class);

    when(group.getName()).thenReturn(DEFAULT_GROUP_NAME);
    when(group.getModelProperty(ParameterGroupModelProperty.class)).thenReturn(empty());
    when(group.getParameterModels()).thenReturn(parameters);
    when(group.isShowInDsl()).thenReturn(true);

    when(model.getParameterGroupModels()).thenReturn(asList(group));
  }

  private void mockParameterGroups(ParameterizedModel model, boolean showInDsl) {
    ParameterGroupModel group = mock(ParameterGroupModel.class);
    ParameterModel parameterModel = getParameter(SIMPLE_PARAM_NAME, Object.class);

    when(group.getName()).thenReturn(DEFAULT_GROUP_NAME);
    when(group.getModelProperty(ParameterGroupModelProperty.class)).thenReturn(empty());
    when(group.getParameterModels()).thenReturn(asList(parameterModel));

    ParameterGroupModel anotherGroup = mock(ParameterGroupModel.class);
    when(anotherGroup.getName()).thenReturn("My Group");
    when(anotherGroup.isShowInDsl()).thenReturn(showInDsl);
    when(anotherGroup.getParameterModels()).thenReturn(asList(parameterModel));

    when(model.getParameterGroupModels()).thenReturn(asList(group, anotherGroup));
  }

  private void validate() {
    ExtensionsTestUtils.validate(extensionModel, validator);
  }

  @TypeDsl(allowTopLevelDefinition = true)
  public static class TopLevelTest {

    public TopLevelTest() {

    }

    private String id;

    public String getId() {
      return id;
    }
  }

  @Alias("top-level-test")
  public static class TopLevelSubtype {

    public TopLevelSubtype() {

    }

    private String id;

    public String getId() {
      return id;
    }
  }

  @TypeDsl(allowTopLevelDefinition = true)
  public static class TopLevelConfig {

    public TopLevelConfig() {

    }

    private String id;

    public String getId() {
      return id;
    }
  }

  public static class ChildTest {

    public ChildTest() {

    }

    private String id;

    public String getId() {
      return id;
    }
  }

  public static class NoChildTest {

    private String id;

    public String getId() {
      return id;
    }
  }

  public static class ChildElementTest {

    @Parameter
    String argument1;

    @Parameter
    String argument2;

    @Parameter
    @Alias(value = REPEATED_NAME)
    ChildObjectTest repeatedName;
  }

  public static class ChildObjectTest {

    @Parameter
    String argument3;
  }

  public static class ChildElementTestClone {

    @Parameter
    String argument1;

    @Parameter
    String argument2;

    @Parameter
    @Alias(value = REPEATED_NAME)
    ChildObjectTestClone repeatedName;
  }

  public static class ChildObjectTestClone {

    @Parameter
    String argument3;
    @Parameter
    String argument4;
  }

}
