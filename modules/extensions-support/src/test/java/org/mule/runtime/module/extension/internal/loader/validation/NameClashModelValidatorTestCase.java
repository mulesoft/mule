/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.lang.String.format;
import static java.util.Arrays.asList;
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
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.CONTENT;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.PRIMARY_CONTENT;
import static org.mule.runtime.api.util.ExtensionModelTestUtils.visitableMock;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.getParameter;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockParameters;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceCallbackModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class NameClashModelValidatorTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private static final String OPERATION_NAME = "operation";
  private static final String TOP_LEVEL_OPERATION_PARAM_NAME = "topLevelOperationParam";
  private static final String CONFIG_NAME = "config";
  private static final String SOURCE_NAME = "source";
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

  @Mock
  private SourceModel sourceModel;

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

  private MetadataType childTestMap = baseTypeBuilder.objectType()
      .id(HashMap.class.getName())
      .openWith(toMetadataType(ChildTest.class))
      .with(new ClassInformationAnnotation(HashMap.class, asList(String.class, ChildTest.class)))
      .build();

  private MetadataType topLevelMap = baseTypeBuilder.objectType()
      .id(HashMap.class.getName())
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
    when(extensionModel.getXmlDslModel()).thenReturn(xmlDslModel);

    simpleConfigParam = getParameter(SIMPLE_PARAM_NAME, String.class);
    topLevelConfigParam = getParameter(TOP_LEVEL_OPERATION_PARAM_NAME, TopLevelTest.class);

    simpleOperationParam = getParameter(SIMPLE_PARAM_NAME, String.class);
    topLevelOperationParam = getParameter(TOP_LEVEL_OPERATION_PARAM_NAME, TopLevelTest.class);

    simpleConnectionProviderParam = getParameter(SIMPLE_PARAM_NAME, String.class);
    topLevelConnectionProviderParam = getParameter(TOP_LEVEL_OPERATION_PARAM_NAME, TopLevelTest.class);

    when(configurationModel.getName()).thenReturn(CONFIG_NAME);
    when(configurationModel.getAllParameterModels()).thenReturn(asList(simpleConfigParam, topLevelConfigParam));
    when(configurationModel.getOperationModels()).thenReturn(ImmutableList.of());
    when(configurationModel.getConnectionProviders()).thenReturn(ImmutableList.of());
    when(configurationModel.getParameterGroupModels()).thenReturn(ImmutableList.of());

    when(operationModel.getName()).thenReturn(OPERATION_NAME);
    when(operationModel.getAllParameterModels()).thenReturn(asList(simpleOperationParam, topLevelOperationParam));
    mockModelProperties(operationModel);
    mockModelProperties(configurationModel);
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
    when(configurationModel.getAllParameterModels()).thenReturn(asList(simpleConfigParam, topLevelConfigParam, offending));
    validate();
  }

  @Test
  public void configNameClashesWithOperationParameterType() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(SIMPLE_PARAM_NAME, TopLevelTest.class);
    when(configurationModel.getName()).thenReturn(TopLevelTest.class.getName());
    mockParameters(operationModel, topLevelConfigParam, offending);
    validate();
  }

  @Test
  public void connectionProviderTopLevelParameterCrashesWithOperationName() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(OPERATION_NAME, TopLevelTest.class);
    mockParameters(connectionProviderModel, simpleConnectionProviderParam, topLevelConnectionProviderParam, offending);
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
    when(operationModel.getAllParameterModels()).thenReturn(asList(topLevelOperationParam, offending));
    validate();
  }

  @Test
  public void connectionProviderNameClashesWithOperationParameterType() {
    exception.expect(IllegalModelDefinitionException.class);
    when(connectionProviderModel.getName()).thenReturn(SIMPLE_PARAM_NAME);
    ParameterModel offending = getParameter(connectionProviderModel.getName(), TopLevelTest.class);
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
    when(configurationModel.getAllParameterModels()).thenReturn(asList(simpleConfigParam, topLevelConfigParam, offending));
    validate();
  }

  @Test
  public void operationWithRepeatedParameterName() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(SIMPLE_PARAM_NAME, String.class);
    when(operationModel.getAllParameterModels()).thenReturn(asList(simpleOperationParam, topLevelOperationParam, offending));
    validate();
  }

  @Test
  public void operationNameClashesWithParameterTypeName() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(TOP_LEVEL_SINGULAR_PARAM_NAME, TopLevelTest.class);
    when(operationModel.getName()).thenReturn(TopLevelTest.class.getName());
    mockParameters(operationModel, topLevelOperationParam, offending);
    validate();
  }

  @Test
  public void connectionProviderWithRepeatedParameterName() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(SIMPLE_PARAM_NAME, String.class);
    when(connectionProviderModel.getAllParameterModels())
        .thenReturn(asList(simpleConnectionProviderParam, topLevelConnectionProviderParam, offending));
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
    when(sourceCallbackModel.getAllParameterModels()).thenReturn(asList(simpleConnectionProviderParam, offending));
    when(sourceModel.getErrorCallback()).thenReturn(of(sourceCallbackModel));
    validate();
  }

  @Test
  public void sourceWithRepeatedParameterNameAmongCallbackAndSource() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel offending = getParameter(SIMPLE_PARAM_NAME, String.class);
    SourceCallbackModel sourceCallbackModel = mock(SourceCallbackModel.class);
    when(sourceCallbackModel.getAllParameterModels()).thenReturn(asList(simpleConfigParam));

    ParameterGroupModel group = mock(ParameterGroupModel.class);
    when(group.getName()).thenReturn(DEFAULT_GROUP_NAME);
    when(group.getModelProperty(ParameterGroupModelProperty.class)).thenReturn(empty());
    when(group.getParameterModels()).thenReturn(asList(offending));

    SourceModel sourceModel = new ImmutableSourceModel(SOURCE_NAME, "", false, asList(group), null, null,
                                                       of(sourceCallbackModel), empty(), empty(), false, false, false, null,
                                                       emptySet(), emptySet(), emptySet());
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
    when(connectionProviderModel.getName()).thenReturn(CHILD_SINGULAR_PARAM_NAME);
    ParameterModel offending = getParameter(CHILD_PLURAL_PARAM_NAME, childTestMap);
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
    exception.expect(IllegalModelDefinitionException.class);
    when(connectionProviderModel.getName()).thenReturn(CHILD_SINGULAR_PARAM_NAME);
    ParameterModel offending = getParameter(CHILD_PLURAL_PARAM_NAME, childTestList);
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
  public void contentParamtersWithSameNameAndDifferentType() {
    exception.expect(IllegalModelDefinitionException.class);
    ParameterModel firtParam = getParameter(CHILD_SINGULAR_PARAM_NAME, Object.class);
    when(firtParam.getRole()).thenReturn(PRIMARY_CONTENT);
    ParameterModel secondParam = getParameter(CHILD_SINGULAR_PARAM_NAME, TopLevelTest.class);
    when(secondParam.getRole()).thenReturn(CONTENT);
    when(operationModel.getAllParameterModels()).thenReturn(asList(firtParam));
    when(sourceModel.getAllParameterModels()).thenReturn(asList(secondParam));
    validate();
  }

  private void validate() {
    ExtensionsTestUtils.validate(extensionModel, validator);
  }

  @XmlHints(allowTopLevelDefinition = true)
  private static class TopLevelTest {

    public TopLevelTest() {

    }

    private String id;

    public String getId() {
      return id;
    }
  }

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
