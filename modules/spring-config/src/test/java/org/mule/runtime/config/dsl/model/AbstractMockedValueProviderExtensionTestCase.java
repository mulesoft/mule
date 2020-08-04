/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model;

import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.IntStream.range;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.api.util.ExtensionModelTestUtils.visitableMock;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newParameterGroup;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULE_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.ERROR_MAPPINGS_PARAMETER_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.FLOW_ELEMENT_IDENTIFIER;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ERROR_MAPPINGS;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ValueProviderModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.type.TypeCatalog;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.ConfigurationElementDeclaration;
import org.mule.runtime.app.declaration.api.ConnectionElementDeclaration;
import org.mule.runtime.app.declaration.api.ElementDeclaration;
import org.mule.runtime.app.declaration.api.OperationElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterValue;
import org.mule.runtime.app.declaration.api.fluent.ElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.ParameterListValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterObjectValue;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.api.dsl.processor.ArtifactConfig;
import org.mule.runtime.config.internal.model.ApplicationModel;
import org.mule.runtime.core.api.extension.MuleExtensionModelProvider;
import org.mule.runtime.core.internal.exception.ErrorMapping;
import org.mule.runtime.core.internal.locator.ComponentLocator;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheId;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.property.RequiredForMetadataModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;

import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Before;
import org.mockito.Mock;
import org.slf4j.Logger;

public abstract class AbstractMockedValueProviderExtensionTestCase extends AbstractMuleTestCase {

  private static final Logger LOGGER = getLogger(AbstractMockedValueProviderExtensionTestCase.class);

  protected static final String NAMESPACE = "vp-mockns";
  protected static final String NAMESPACE_URI = "http://www.mulesoft.org/schema/mule/vp-mockns";
  protected static final String SCHEMA_LOCATION = "http://www.mulesoft.org/schema/mule/vp-mockns/current/vp-mule-mockns.xsd";
  protected static final String PARAMETER_IN_GROUP_NAME = "parameterInGroup";
  protected static final String PARAMETER_IN_GROUP_DEFAULT_VALUE = "parameterInGroup";
  protected static final String CUSTOM_PARAMETER_GROUP_NAME = "customParameterGroupName";
  protected static final String ACTING_PARAMETER_NAME = "actingParameter";
  protected static final String ACTING_PARAMETER_DEFAULT_VALUE = "actingParameter";
  protected static final String PARAMETER_REQUIRED_FOR_METADATA_NAME = "requiredForMetadata";
  protected static final String PARAMETER_REQUIRED_FOR_METADATA_DEFAULT_VALUE = "requiredForMetadata";
  protected static final String PROVIDED_PARAMETER_NAME = "providedParameter";
  protected static final String OTHER_PROVIDED_PARAMETER_NAME = "otherProvidedParameter";
  protected static final String PROVIDED_FROM_COMPLEX_PARAMETER_NAME = "fromComplexActingParameter";
  protected static final String COMPLEX_ACTING_PARAMETER_NAME = "complexActingParameter";
  protected static final String PROVIDED_PARAMETER_DEFAULT_VALUE = "providedParameter";
  protected static final String EXTENSION_NAME = "extension";
  protected static final String OPERATION_NAME = "mockOperation";
  protected static final String OTHER_OPERATION_NAME = "mockOtherOperation";
  protected static final String SOURCE_NAME = "source";
  protected static final String CONFIGURATION_NAME = "configuration";
  protected static final String OTHER_CONFIGURATION_NAME = "otherConfiguration";
  protected static final String CONNECTION_PROVIDER_NAME = "connection";
  protected static final String VALUE_PROVIDER_NAME = "valueProvider";
  protected static final String COMPLEX_VALUE_PROVIDER_NAME = "complexValueProvider";

  protected static final String MY_FLOW = "myFlow";
  protected static final String MY_CONFIG = "myConfig";
  protected static final String MY_CONNECTION = MY_CONFIG + "/connection"; // Not a valid location, hack to reuse helper function.
  protected static final String SOURCE_LOCATION = MY_FLOW + "/source";
  protected static final String OPERATION_LOCATION = MY_FLOW + "/processors/0";
  protected static final String OTHER_OPERATION_LOCATION = MY_FLOW + "/processors/1";

  @Mock(lenient = true)
  protected ExtensionModel mockExtension;

  @Mock(lenient = true)
  protected ConfigurationModel configuration;

  @Mock(lenient = true)
  protected ConfigurationModel otherConfiguration;

  @Mock(lenient = true)
  protected OperationModel operation;

  @Mock(lenient = true)
  protected OperationModel otherOperation;

  @Mock(lenient = true)
  protected ConnectionProviderModel connectionProvider;

  @Mock(answer = RETURNS_DEEP_STUBS, lenient = true)
  protected SourceModel source;

  @Mock(lenient = true)
  protected ParameterModel parameterInGroup;

  @Mock(lenient = true)
  protected ParameterGroupModel actingParametersGroup;

  @Mock(lenient = true)
  protected ParameterModel nameParameter;

  @Mock(lenient = true)
  protected ParameterModel actingParameter;

  @Mock(lenient = true)
  protected ParameterModel providedParameter;

  @Mock(lenient = true)
  protected ParameterModel otherProvidedParameter;

  @Mock(lenient = true)
  protected ParameterModel providedParameterFromComplex;

  @Mock(lenient = true)
  protected ParameterModel complexActingParameter;

  @Mock(lenient = true)
  protected ParameterGroupModel parameterGroup;

  @Mock(lenient = true)
  protected ParameterModel errorMappingsParameter;

  @Mock(lenient = true)
  protected ParameterGroupModel errorMappingsParameterGroup;

  @Mock(lenient = true)
  protected ParameterModel parameterRequiredForMetadata;

  @Mock(lenient = true)
  protected DslResolvingContext dslContext;

  @Mock(lenient = true)
  protected ValueProviderModel valueProviderModel;

  @Mock(lenient = true)
  protected ValueProviderModel complexValueProviderModel;

  protected ClassTypeLoader TYPE_LOADER = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
  protected List<ParameterModel> customParameterGroupModels;
  protected List<ParameterModel> defaultGroupParameterModels;

  private Set<ExtensionModel> extensions;
  private ElementDeclarer declarer;

  @Before
  public void before() {
    initMocks(this);

    initializeExtensionMock(mockExtension);

    when(nameParameter.getName()).thenReturn("name");
    when(nameParameter.getExpressionSupport()).thenReturn(NOT_SUPPORTED);
    when(nameParameter.getModelProperty(any())).thenReturn(empty());
    when(nameParameter.getDslConfiguration()).thenReturn(ParameterDslConfiguration.getDefaultInstance());
    when(nameParameter.getLayoutModel()).thenReturn(empty());
    when(nameParameter.getRole()).thenReturn(BEHAVIOUR);
    when(nameParameter.getType()).thenReturn(TYPE_LOADER.load(String.class));
    when(nameParameter.isComponentId()).thenReturn(true);

    when(valueProviderModel.getPartOrder()).thenReturn(0);
    when(valueProviderModel.getProviderName()).thenReturn(VALUE_PROVIDER_NAME);
    when(valueProviderModel.getActingParameters()).thenReturn(asList(ACTING_PARAMETER_NAME, PARAMETER_IN_GROUP_NAME));
    when(valueProviderModel.requiresConfiguration()).thenReturn(false);
    when(valueProviderModel.requiresConnection()).thenReturn(false);

    when(complexValueProviderModel.getPartOrder()).thenReturn(0);
    when(complexValueProviderModel.getProviderName()).thenReturn(COMPLEX_VALUE_PROVIDER_NAME);
    when(complexValueProviderModel.getActingParameters()).thenReturn(asList(COMPLEX_ACTING_PARAMETER_NAME));
    when(complexValueProviderModel.requiresConfiguration()).thenReturn(false);
    when(complexValueProviderModel.requiresConnection()).thenReturn(false);

    when(parameterInGroup.getName()).thenReturn(PARAMETER_IN_GROUP_NAME);
    when(parameterInGroup.getExpressionSupport()).thenReturn(NOT_SUPPORTED);
    when(parameterInGroup.getModelProperty(any())).thenReturn(empty());
    when(parameterInGroup.getDslConfiguration()).thenReturn(ParameterDslConfiguration.getDefaultInstance());
    when(parameterInGroup.getLayoutModel()).thenReturn(empty());
    when(parameterInGroup.getRole()).thenReturn(BEHAVIOUR);
    when(parameterInGroup.getType()).thenReturn(TYPE_LOADER.load(String.class));

    when(actingParameter.getName()).thenReturn(ACTING_PARAMETER_NAME);
    when(actingParameter.getExpressionSupport()).thenReturn(NOT_SUPPORTED);
    when(actingParameter.getModelProperty(any())).thenReturn(empty());
    when(actingParameter.getDslConfiguration()).thenReturn(ParameterDslConfiguration.getDefaultInstance());
    when(actingParameter.getLayoutModel()).thenReturn(empty());
    when(actingParameter.getRole()).thenReturn(BEHAVIOUR);
    when(actingParameter.getType()).thenReturn(TYPE_LOADER.load(String.class));

    when(providedParameter.getName()).thenReturn(PROVIDED_PARAMETER_NAME);
    when(providedParameter.getExpressionSupport()).thenReturn(NOT_SUPPORTED);
    when(providedParameter.getModelProperty(any())).thenReturn(empty());
    when(providedParameter.getDslConfiguration()).thenReturn(ParameterDslConfiguration.getDefaultInstance());
    when(providedParameter.getLayoutModel()).thenReturn(empty());
    when(providedParameter.getRole()).thenReturn(BEHAVIOUR);
    when(providedParameter.getType()).thenReturn(TYPE_LOADER.load(String.class));
    when(providedParameter.getValueProviderModel()).thenReturn(of(valueProviderModel));

    when(otherProvidedParameter.getName()).thenReturn(OTHER_PROVIDED_PARAMETER_NAME);
    when(otherProvidedParameter.getExpressionSupport()).thenReturn(NOT_SUPPORTED);
    when(otherProvidedParameter.getModelProperty(any())).thenReturn(empty());
    when(otherProvidedParameter.getDslConfiguration()).thenReturn(ParameterDslConfiguration.getDefaultInstance());
    when(otherProvidedParameter.getLayoutModel()).thenReturn(empty());
    when(otherProvidedParameter.getRole()).thenReturn(BEHAVIOUR);
    when(otherProvidedParameter.getType()).thenReturn(TYPE_LOADER.load(String.class));
    when(otherProvidedParameter.getValueProviderModel()).thenReturn(of(valueProviderModel));

    when(providedParameterFromComplex.getName()).thenReturn(PROVIDED_FROM_COMPLEX_PARAMETER_NAME);
    when(providedParameterFromComplex.getExpressionSupport()).thenReturn(NOT_SUPPORTED);
    when(providedParameterFromComplex.getModelProperty(any())).thenReturn(empty());
    when(providedParameterFromComplex.getDslConfiguration()).thenReturn(ParameterDslConfiguration.getDefaultInstance());
    when(providedParameterFromComplex.getLayoutModel()).thenReturn(empty());
    when(providedParameterFromComplex.getRole()).thenReturn(BEHAVIOUR);
    when(providedParameterFromComplex.getType()).thenReturn(TYPE_LOADER.load(String.class));
    when(providedParameterFromComplex.getValueProviderModel()).thenReturn(of(complexValueProviderModel));

    when(complexActingParameter.getName()).thenReturn(COMPLEX_ACTING_PARAMETER_NAME);
    when(complexActingParameter.getExpressionSupport()).thenReturn(NOT_SUPPORTED);
    when(complexActingParameter.getModelProperty(any())).thenReturn(empty());
    when(complexActingParameter.getDslConfiguration()).thenReturn(ParameterDslConfiguration.getDefaultInstance());
    when(complexActingParameter.getLayoutModel()).thenReturn(empty());
    when(complexActingParameter.getRole()).thenReturn(BEHAVIOUR);
    when(complexActingParameter.getType()).thenReturn(TYPE_LOADER.load(ComplexActingParameter.class));

    when(parameterRequiredForMetadata.getName()).thenReturn(PARAMETER_REQUIRED_FOR_METADATA_NAME);
    when(parameterRequiredForMetadata.getExpressionSupport()).thenReturn(NOT_SUPPORTED);
    when(parameterRequiredForMetadata.getModelProperty(any())).thenReturn(empty());
    when(parameterRequiredForMetadata.getDslConfiguration()).thenReturn(ParameterDslConfiguration.getDefaultInstance());
    when(parameterRequiredForMetadata.getLayoutModel()).thenReturn(empty());
    when(parameterRequiredForMetadata.getRole()).thenReturn(BEHAVIOUR);
    when(parameterRequiredForMetadata.getType()).thenReturn(TYPE_LOADER.load(String.class));

    this.customParameterGroupModels = asList(parameterInGroup);
    when(actingParametersGroup.getName()).thenReturn(CUSTOM_PARAMETER_GROUP_NAME);
    when(actingParametersGroup.isShowInDsl()).thenReturn(false);
    when(actingParametersGroup.getParameterModels()).thenReturn(customParameterGroupModels);
    when(actingParametersGroup.getParameter(anyString())).then(invocation -> {
      String paramName = invocation.getArgument(0);
      if (PARAMETER_IN_GROUP_NAME.equals(paramName)) {
        return of(parameterInGroup);
      }
      return empty();
    });

    when(errorMappingsParameter.getName()).thenReturn(ERROR_MAPPINGS_PARAMETER_NAME);
    when(errorMappingsParameter.getExpressionSupport()).thenReturn(NOT_SUPPORTED);
    when(errorMappingsParameter.getModelProperty(any())).thenReturn(empty());
    when(errorMappingsParameter.getDslConfiguration()).thenReturn(ParameterDslConfiguration.getDefaultInstance());
    when(errorMappingsParameter.getLayoutModel()).thenReturn(empty());
    when(errorMappingsParameter.getRole()).thenReturn(BEHAVIOUR);
    when(errorMappingsParameter.getType()).thenReturn(BaseTypeBuilder.create(JAVA).arrayType()
        .of(TYPE_LOADER.load(ErrorMapping.class)).build());

    when(errorMappingsParameterGroup.getName()).thenReturn(ERROR_MAPPINGS);
    when(errorMappingsParameterGroup.isShowInDsl()).thenReturn(false);
    when(errorMappingsParameterGroup.getParameterModels()).thenReturn(asList(errorMappingsParameter));
    when(errorMappingsParameterGroup.getParameter(ERROR_MAPPINGS_PARAMETER_NAME)).thenReturn(of(errorMappingsParameter));

    this.defaultGroupParameterModels = asList(nameParameter,
                                              actingParameter,
                                              providedParameter,
                                              parameterRequiredForMetadata,
                                              complexActingParameter,
                                              providedParameterFromComplex);
    when(parameterGroup.getName()).thenReturn(DEFAULT_GROUP_NAME);
    when(parameterGroup.isShowInDsl()).thenReturn(false);
    when(parameterGroup.getParameterModels()).thenReturn(defaultGroupParameterModels);
    when(parameterGroup.getParameter(anyString())).then(invocation -> {
      String paramName = invocation.getArgument(0);
      switch (paramName) {
        case ACTING_PARAMETER_NAME:
          return of(actingParameter);
        case PROVIDED_PARAMETER_NAME:
          return of(providedParameter);
        case PARAMETER_REQUIRED_FOR_METADATA_NAME:
          return of(parameterRequiredForMetadata);
        case OTHER_PROVIDED_PARAMETER_NAME:
          return of(otherProvidedParameter);
        case PROVIDED_FROM_COMPLEX_PARAMETER_NAME:
          return of(providedParameterFromComplex);
        case COMPLEX_ACTING_PARAMETER_NAME:
          return of(complexActingParameter);
      }
      return empty();
    });

    RequiredForMetadataModelProperty requiredForMetadataModelProperty =
        new RequiredForMetadataModelProperty(asList(PARAMETER_REQUIRED_FOR_METADATA_NAME));

    when(connectionProvider.getName()).thenReturn(CONNECTION_PROVIDER_NAME);
    when(connectionProvider.getParameterGroupModels()).thenReturn(asList(parameterGroup, actingParametersGroup));
    when(connectionProvider.getModelProperty(RequiredForMetadataModelProperty.class))
        .thenReturn(of(requiredForMetadataModelProperty));

    when(configuration.getName()).thenReturn(CONFIGURATION_NAME);
    when(configuration.getParameterGroupModels()).thenReturn(asList(parameterGroup, actingParametersGroup));
    when(configuration.getOperationModels()).thenReturn(asList(operation, otherOperation));
    when(configuration.getSourceModels()).thenReturn(asList(source));
    when(configuration.getConnectionProviders()).thenReturn(asList(connectionProvider));
    when(configuration.getConnectionProviderModel(CONNECTION_PROVIDER_NAME)).thenReturn(of(connectionProvider));
    when(configuration.getModelProperty(RequiredForMetadataModelProperty.class)).thenReturn(of(requiredForMetadataModelProperty));

    when(otherConfiguration.getName()).thenReturn(OTHER_CONFIGURATION_NAME);
    when(otherConfiguration.getParameterGroupModels()).thenReturn(asList(parameterGroup, actingParametersGroup));
    when(otherConfiguration.getOperationModels()).thenReturn(emptyList());
    when(otherConfiguration.getSourceModels()).thenReturn(emptyList());
    when(otherConfiguration.getConnectionProviders()).thenReturn(asList(connectionProvider));
    when(otherConfiguration.getModelProperty(RequiredForMetadataModelProperty.class))
        .thenReturn(of(requiredForMetadataModelProperty));

    when(source.getName()).thenReturn(SOURCE_NAME);
    when(source.getParameterGroupModels()).thenReturn(asList(parameterGroup, actingParametersGroup));
    when(source.getSuccessCallback()).thenReturn(empty());
    when(source.getErrorCallback()).thenReturn(empty());

    when(operation.getName()).thenReturn(OPERATION_NAME);
    when(operation.getParameterGroupModels())
        .thenReturn(asList(parameterGroup, actingParametersGroup, errorMappingsParameterGroup));

    when(otherOperation.getName()).thenReturn(OTHER_OPERATION_NAME);
    when(otherOperation.getParameterGroupModels())
        .thenReturn(asList(parameterGroup, actingParametersGroup, errorMappingsParameterGroup));

    visitableMock(operation, otherOperation, source);

    when(dslContext.getExtension(any())).thenReturn(of(mockExtension));
    when(dslContext.getExtensions()).thenReturn(singleton(mockExtension));

    List<ParameterModel> allParameterModels = new ArrayList<>();
    allParameterModels.addAll(defaultGroupParameterModels);
    allParameterModels.addAll(customParameterGroupModels);

    Stream.of(configuration, otherConfiguration, operation, otherOperation, connectionProvider, source)
        .forEach(model -> when(model.getAllParameterModels()).thenReturn(allParameterModels));

    extensions = ImmutableSet.<ExtensionModel>builder()
        .add(MuleExtensionModelProvider.getExtensionModel())
        .add(mockExtension)
        .build();

    TypeCatalog typeCatalog = DslResolvingContext.getDefault(extensions).getTypeCatalog();

    when(dslContext.getTypeCatalog()).thenReturn(typeCatalog);

    declarer = ElementDeclarer.forExtension(EXTENSION_NAME);
  }

  protected void initializeExtensionMock(ExtensionModel extension) {
    when(extension.getName()).thenReturn(EXTENSION_NAME);
    when(extension.getXmlDslModel()).thenReturn(XmlDslModel.builder()
        .setXsdFileName("mule-mockns.xsd")
        .setPrefix(NAMESPACE)
        .setNamespace(NAMESPACE_URI)
        .setSchemaLocation(SCHEMA_LOCATION)
        .setSchemaVersion("4.0")
        .build());
    when(extension.getSubTypes()).thenReturn(emptySet());
    when(extension.getImportedTypes()).thenReturn(emptySet());
    when(extension.getXmlDslModel()).thenReturn(XmlDslModel.builder()
        .setXsdFileName(EMPTY)
        .setPrefix(NAMESPACE)
        .setNamespace(NAMESPACE_URI)
        .setSchemaLocation(SCHEMA_LOCATION)
        .setSchemaVersion(EMPTY)
        .build());

    when(extension.getConfigurationModels()).thenReturn(asList(configuration, otherConfiguration));
    when(extension.getConfigurationModel(anyString())).then(invocation -> {
      if (configuration.getName().equals(invocation.getArgument(0))) {
        return of(configuration);
      } else if (otherConfiguration.getName().equals(invocation.getArgument(0))) {
        return of(otherConfiguration);
      }
      return empty();
    });
    when(extension.getOperationModels()).thenReturn(asList(operation, otherOperation));
    when(extension.getOperationModel(eq(OPERATION_NAME))).thenReturn(of(operation));
    when(extension.getOperationModel(eq(OTHER_OPERATION_NAME))).thenReturn(of(otherOperation));

    when(extension.getSourceModels()).thenReturn(asList(source));
    when(extension.getSourceModel(anyString())).thenReturn(of(source));
    when(extension.getConnectionProviders()).thenReturn(asList(connectionProvider));
    when(extension.getConnectionProviderModel(anyString())).thenReturn(of(connectionProvider));
  }

  protected ConfigurationElementDeclaration declareConfig(ConnectionElementDeclaration connectionDeclaration, String name,
                                                          String parameterRequiredForMetadata, String actingParameter,
                                                          String providedParameter, String parameterInGroup) {
    return declarer.newConfiguration(CONFIGURATION_NAME)
        .withRefName(name)
        .withParameterGroup(newParameterGroup()
            .withParameter(PARAMETER_REQUIRED_FOR_METADATA_NAME, parameterRequiredForMetadata)
            .withParameter(ACTING_PARAMETER_NAME, actingParameter)
            .withParameter(PROVIDED_PARAMETER_NAME, providedParameter)
            .getDeclaration())
        .withParameterGroup(newParameterGroup(CUSTOM_PARAMETER_GROUP_NAME)
            .withParameter(PARAMETER_IN_GROUP_NAME, parameterInGroup)
            .getDeclaration())
        .withConnection(connectionDeclaration)
        .getDeclaration();
  }

  protected ConfigurationElementDeclaration declareOtherConfig(ConnectionElementDeclaration connectionDeclaration, String name,
                                                               String parameterRequiredForMetadata, String actingParameter,
                                                               String providedParameter, String parameterInGroup) {
    return declarer.newConfiguration(OTHER_CONFIGURATION_NAME)
        .withRefName(name)
        .withParameterGroup(newParameterGroup()
            .withParameter(PARAMETER_REQUIRED_FOR_METADATA_NAME, parameterRequiredForMetadata)
            .withParameter(ACTING_PARAMETER_NAME, actingParameter)
            .withParameter(PROVIDED_PARAMETER_NAME, providedParameter)
            .getDeclaration())
        .withParameterGroup(newParameterGroup(CUSTOM_PARAMETER_GROUP_NAME)
            .withParameter(PARAMETER_IN_GROUP_NAME, parameterInGroup)
            .getDeclaration())
        .withConnection(connectionDeclaration)
        .getDeclaration();
  }

  protected ConnectionElementDeclaration declareConnection(String parameterRequiredForMetadata, String actingParameter,
                                                           String providedParameter, String parameterInGroup) {
    return declarer.newConnection(CONNECTION_PROVIDER_NAME)
        .withParameterGroup(newParameterGroup()
            .withParameter(PARAMETER_REQUIRED_FOR_METADATA_NAME, parameterRequiredForMetadata)
            .withParameter(ACTING_PARAMETER_NAME, actingParameter)
            .withParameter(PROVIDED_PARAMETER_NAME, providedParameter)
            .getDeclaration())
        .withParameterGroup(newParameterGroup(CUSTOM_PARAMETER_GROUP_NAME)
            .withParameter(PARAMETER_IN_GROUP_NAME, parameterInGroup)
            .getDeclaration())
        .getDeclaration();
  }

  protected ParameterValue newComplexActingParameter(int intParam,
                                                     String stringParam,
                                                     List<String> listParam,
                                                     int innerIntParam,
                                                     String innerStringParam,
                                                     List<String> innerListParam) {
    ParameterListValue.Builder listValueBuilder = ParameterListValue.builder();
    listParam.forEach(listValueBuilder::withValue);
    ParameterListValue.Builder innerListValueBuilder = ParameterListValue.builder();
    innerListParam.forEach(innerListValueBuilder::withValue);
    return ParameterObjectValue.builder()
        .withParameter("innerPojoParam",
                       ParameterObjectValue.builder()
                           .withParameter("intParam", Integer.toString(innerIntParam))
                           .withParameter("stringParam", innerStringParam)
                           .withParameter("listParam", innerListValueBuilder.build())
                           .build())
        .withParameter("intParam", Integer.toString(intParam))
        .withParameter("stringParam", stringParam)
        .withParameter("listParam", listValueBuilder.build())
        .build();

  }

  public OperationElementDeclaration declareOperation(String operationName) {
    return declarer.newOperation(operationName)
        .withConfig(MY_CONFIG)
        .withParameterGroup(newParameterGroup()
            .withParameter(ACTING_PARAMETER_NAME, ACTING_PARAMETER_DEFAULT_VALUE)
            .withParameter(PROVIDED_PARAMETER_NAME, PROVIDED_PARAMETER_DEFAULT_VALUE)
            .withParameter(COMPLEX_ACTING_PARAMETER_NAME, newComplexActingParameter(
                                                                                    0,
                                                                                    "zero",
                                                                                    asList("one", "two", "three"),
                                                                                    0,
                                                                                    "zero",
                                                                                    asList("one", "two", "three")))
            .withParameter(PROVIDED_FROM_COMPLEX_PARAMETER_NAME, PROVIDED_PARAMETER_DEFAULT_VALUE)
            .getDeclaration())

        .withParameterGroup(newParameterGroup(CUSTOM_PARAMETER_GROUP_NAME)
            .withParameter(PARAMETER_IN_GROUP_NAME, PARAMETER_IN_GROUP_DEFAULT_VALUE)
            .getDeclaration())
        .getDeclaration();
  }


  protected ArtifactDeclaration getBaseApp() {
    return ElementDeclarer.newArtifact()
        .withGlobalElement(
                           declareConfig(
                                         declareConnection(PARAMETER_REQUIRED_FOR_METADATA_DEFAULT_VALUE,
                                                           ACTING_PARAMETER_DEFAULT_VALUE,
                                                           PROVIDED_PARAMETER_DEFAULT_VALUE,
                                                           PARAMETER_IN_GROUP_DEFAULT_VALUE),
                                         MY_CONFIG,
                                         PARAMETER_REQUIRED_FOR_METADATA_DEFAULT_VALUE,
                                         ACTING_PARAMETER_DEFAULT_VALUE,
                                         PROVIDED_PARAMETER_DEFAULT_VALUE,
                                         PARAMETER_IN_GROUP_DEFAULT_VALUE))
        .withGlobalElement(ElementDeclarer.forExtension(MULE_NAME)
            .newConstruct(FLOW_ELEMENT_IDENTIFIER)
            .withRefName(MY_FLOW)
            .withComponent(
                           declarer.newSource(SOURCE_NAME).withConfig(MY_CONFIG)
                               .withParameterGroup(newParameterGroup()
                                   .withParameter(ACTING_PARAMETER_NAME, ACTING_PARAMETER_DEFAULT_VALUE)
                                   .withParameter(PROVIDED_PARAMETER_NAME, PROVIDED_PARAMETER_DEFAULT_VALUE)
                                   .getDeclaration())
                               .withParameterGroup(newParameterGroup(CUSTOM_PARAMETER_GROUP_NAME)
                                   .withParameter(PARAMETER_IN_GROUP_NAME, PARAMETER_IN_GROUP_DEFAULT_VALUE)
                                   .getDeclaration())
                               .getDeclaration())
            .withComponent(declareOperation(OPERATION_NAME))
            .withComponent(declareOperation(OTHER_OPERATION_NAME))
            .getDeclaration())
        .getDeclaration();
  }


  protected ComponentAst getComponentAst(ApplicationModel app, String location) {
    Reference<ComponentAst> componentAst = new Reference<>();
    app.recursiveStream().forEach(c -> {
      if (c.getLocation().getLocation().equals(location)) {
        componentAst.set(c);
      }
    });
    return componentAst.get();
  }

  protected Optional<ElementDeclaration> getDeclaration(ArtifactDeclaration app, String location) {
    return app.findElement(org.mule.runtime.app.declaration.api.component.location.Location
        .builderFromStringRepresentation(location).build());
  }


  protected ApplicationModel loadApplicationModel(ArtifactDeclaration declaration) throws Exception {
    return new ApplicationModel(new ArtifactConfig.Builder().build(),
                                declaration, extensions, emptyMap(), empty(), empty(),
                                uri -> getClass().getResourceAsStream(uri));
  }

  private String collectLog(ValueProviderCacheId valueProviderCacheId, int level) {
    StringBuilder logId = new StringBuilder();
    if (level == 0) {
      logId.append(lineSeparator());
    }
    logId.append(valueProviderCacheId.toString());
    if (!valueProviderCacheId.getParts().isEmpty()) {
      int newLevel = level + 1;
      valueProviderCacheId.getParts().forEach(p -> {
        logId.append(lineSeparator());
        range(0, newLevel).forEach(i -> logId.append(" "));
        logId.append("+-").append(collectLog(p, newLevel));
      });
    }
    return logId.toString();
  }


  protected void checkIdsAreEqual(Optional<ValueProviderCacheId> id1, Optional<ValueProviderCacheId> id2) {
    LOGGER.debug("ID1: " + id1.map(i -> collectLog(i, 0)).orElse("empty"));
    LOGGER.debug("ID2: " + id2.map(i -> collectLog(i, 0)).orElse("empty"));
    assertThat(id1, equalTo(id2));
  }

  protected void checkIdsAreDifferent(Optional<ValueProviderCacheId> id1, Optional<ValueProviderCacheId> id2) {
    LOGGER.debug("ID1: " + id1.map(i -> collectLog(i, 0)).orElse("empty"));
    LOGGER.debug("ID2: " + id2.map(i -> collectLog(i, 0)).orElse("empty"));
    assertThat(id1, not(equalTo(id2)));
  }

  protected static class Locator implements ComponentLocator<ComponentAst> {

    private final Map<Location, ComponentAst> components = new HashMap<>();

    Locator(ApplicationModel app) {
      app.topLevelComponentsStream().forEach(this::addComponent);
    }

    @Override
    public Optional<ComponentAst> get(Location location) {
      return Optional.ofNullable(components.get(location));
    }

    private Location getLocation(ComponentAst component) {
      return Location.builderFromStringRepresentation(component.getLocation().getLocation()).build();
    }

    private void addComponent(ComponentAst component) {
      components.put(getLocation(component), component);
      component.directChildrenStream().forEach(this::addComponent);
    }
  }

}
