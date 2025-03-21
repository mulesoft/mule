/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.dsl.model;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.ComponentVisibility.PUBLIC;
import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.NONE;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder.newStereotype;
import static org.mule.runtime.app.declaration.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newParameterGroup;
import static org.mule.runtime.config.api.dsl.ArtifactDeclarationUtils.toArtifactast;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.FLOW_ELEMENT_IDENTIFIER;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.MULE_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.ERROR_MAPPINGS_PARAMETER_NAME;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.CONFIG;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.CONNECTION;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.PROCESSOR;
import static org.mule.runtime.module.tooling.internal.dsl.model.ComplexActingParameterUtils.DEFAULT_COMPLEX_ACTING_PARAMETER;
import static org.mule.runtime.module.tooling.internal.dsl.model.ComplexActingParameterUtils.declareComplexActingParameter;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ERROR_MAPPINGS;

import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.IntStream.range;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ActingParameterModel;
import org.mule.runtime.api.meta.model.parameter.FieldValueProviderModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterRole;
import org.mule.runtime.api.meta.model.parameter.ValueProviderModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.api.meta.type.TypeCatalog;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.ConfigurationElementDeclaration;
import org.mule.runtime.app.declaration.api.ConnectionElementDeclaration;
import org.mule.runtime.app.declaration.api.ElementDeclaration;
import org.mule.runtime.app.declaration.api.OperationElementDeclaration;
import org.mule.runtime.app.declaration.api.fluent.ElementDeclarer;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.dsl.model.ComplexActingParameter;
import org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheId;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.error.ErrorMapping;
import org.mule.runtime.extension.api.model.ImmutableExtensionModel;
import org.mule.runtime.extension.api.model.config.ImmutableConfigurationModel;
import org.mule.runtime.extension.api.model.connection.ImmutableConnectionProviderModel;
import org.mule.runtime.extension.api.model.operation.ImmutableOperationModel;
import org.mule.runtime.extension.api.model.parameter.ImmutableParameterGroupModel;
import org.mule.runtime.extension.api.model.parameter.ImmutableParameterModel;
import org.mule.runtime.extension.api.model.source.ImmutableSourceModel;
import org.mule.runtime.extension.api.property.RequiredForMetadataModelProperty;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.stereotype.MuleStereotypes;
import org.mule.runtime.metadata.api.locator.ComponentLocator;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import org.slf4j.Logger;

import org.junit.Before;
import org.junit.Rule;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

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
  protected static final String VALUE_PROVIDER_ID = "valueProviderId";
  protected static final String COMPLEX_VALUE_PROVIDER_NAME = "complexValueProvider";
  protected static final String COMPLEX_VALUE_PROVIDER_ID = "complexValueProviderId";
  protected static final String FIELD_VALUE_PROVIDER_NAME = "fieldValueProvider";
  protected static final String FIELD_VALUE_PROVIDER_ID = "fieldValueProviderId";

  protected static final String MY_FLOW = "myFlow";
  protected static final String MY_CONFIG = "myConfig";
  protected static final String MY_CONNECTION = MY_CONFIG + "/connection"; // Not a valid location, hack to reuse helper function.
  protected static final String SOURCE_LOCATION = MY_FLOW + "/source";
  protected static final String OPERATION_LOCATION = MY_FLOW + "/processors/0";
  protected static final String OTHER_OPERATION_LOCATION = MY_FLOW + "/processors/1";

  @Rule
  public MockitoRule mockito = MockitoJUnit.rule();

  @Mock(lenient = true)
  protected ExtensionModel mockExtension;

  protected ConfigurationModel configuration;

  protected ConfigurationModel otherConfiguration;

  protected OperationModel operation;

  protected OperationModel otherOperation;

  protected ConnectionProviderModel connectionProvider;

  protected SourceModel source;

  protected ParameterModel parameterInGroup;

  protected ParameterGroupModel actingParametersGroup;

  protected ParameterModel nameParameter;

  protected ParameterModel configRefParameter;

  protected ParameterModel actingParameter;

  protected ParameterModel providedParameter;

  protected ParameterModel otherProvidedParameter;

  protected ParameterModel providedParameterFromComplex;

  protected ParameterModel complexActingParameter;

  protected ParameterGroupModel componentParameterGroup;

  protected ParameterGroupModel configParameterGroup;

  protected ParameterModel errorMappingsParameter;

  protected ParameterGroupModel errorMappingsParameterGroup;

  protected ParameterModel parameterRequiredForMetadata;

  @Mock(lenient = true)
  protected DslResolvingContext dslContext;

  @Mock(lenient = true)
  protected ValueProviderModel valueProviderModel;

  @Mock(lenient = true)
  protected ValueProviderModel complexValueProviderModel;

  protected ClassTypeLoader TYPE_LOADER = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  private Set<ExtensionModel> extensions;
  private ElementDeclarer declarer;

  @Before
  public void before() {
    final MetadataType stringType = TYPE_LOADER.load(String.class);

    nameParameter =
        createParameterModel("name", true, stringType, null, NOT_SUPPORTED, BEHAVIOUR, null, emptyList());

    configRefParameter = createParameterModel("config-ref", false, TYPE_LOADER.load(ConfigurationProvider.class), null,
                                              NOT_SUPPORTED, BEHAVIOUR, null,
                                              singletonList(newStereotype(CONFIGURATION_NAME, EXTENSION_NAME)
                                                  .withParent(MuleStereotypes.CONFIG).build()));

    when(valueProviderModel.getPartOrder()).thenReturn(0);
    when(valueProviderModel.getProviderName()).thenReturn(VALUE_PROVIDER_NAME);
    when(valueProviderModel.getProviderId()).thenReturn(VALUE_PROVIDER_ID);
    when(valueProviderModel.getActingParameters()).thenReturn(asList(ACTING_PARAMETER_NAME, PARAMETER_IN_GROUP_NAME));

    ActingParameterModel actingParameterModel = createActingParameterModel(ACTING_PARAMETER_NAME);
    ActingParameterModel groupActingParameterModel = createActingParameterModel(PARAMETER_IN_GROUP_NAME);
    when(valueProviderModel.getParameters()).thenReturn(asList(actingParameterModel, groupActingParameterModel));

    when(valueProviderModel.requiresConfiguration()).thenReturn(false);
    when(valueProviderModel.requiresConnection()).thenReturn(false);

    when(complexValueProviderModel.getPartOrder()).thenReturn(0);
    when(complexValueProviderModel.getProviderName()).thenReturn(COMPLEX_VALUE_PROVIDER_NAME);
    when(complexValueProviderModel.getProviderId()).thenReturn(COMPLEX_VALUE_PROVIDER_ID);
    when(complexValueProviderModel.getActingParameters()).thenReturn(singletonList(COMPLEX_ACTING_PARAMETER_NAME));

    ActingParameterModel complexActingParameterModel = createActingParameterModel(COMPLEX_ACTING_PARAMETER_NAME);
    when(complexValueProviderModel.getParameters()).thenReturn(singletonList(complexActingParameterModel));

    when(complexValueProviderModel.requiresConfiguration()).thenReturn(false);
    when(complexValueProviderModel.requiresConnection()).thenReturn(false);

    parameterInGroup = createParameterModel(PARAMETER_IN_GROUP_NAME, false, stringType, null,
                                            NOT_SUPPORTED, BEHAVIOUR, null, emptyList());
    actingParameter = createParameterModel(ACTING_PARAMETER_NAME, false, stringType,
                                           ACTING_PARAMETER_DEFAULT_VALUE, NOT_SUPPORTED, BEHAVIOUR, null, emptyList());
    providedParameter = createParameterModel(PROVIDED_PARAMETER_NAME, false, stringType, null, NOT_SUPPORTED,
                                             BEHAVIOUR, valueProviderModel, emptyList());

    otherProvidedParameter =
        createParameterModel(OTHER_PROVIDED_PARAMETER_NAME, false, stringType, null,
                             NOT_SUPPORTED, BEHAVIOUR, valueProviderModel, emptyList());

    providedParameterFromComplex =
        createParameterModel(PROVIDED_FROM_COMPLEX_PARAMETER_NAME, false, stringType, null,
                             NOT_SUPPORTED, BEHAVIOUR, complexValueProviderModel, emptyList());

    complexActingParameter =
        createParameterModel(COMPLEX_ACTING_PARAMETER_NAME, false, TYPE_LOADER.load(ComplexActingParameter.class), null,
                             NOT_SUPPORTED,
                             BEHAVIOUR, null, emptyList());

    parameterRequiredForMetadata =
        createParameterModel(PARAMETER_REQUIRED_FOR_METADATA_NAME, false, stringType, null, NOT_SUPPORTED,
                             BEHAVIOUR, null, emptyList());

    actingParametersGroup = new ImmutableParameterGroupModel(CUSTOM_PARAMETER_GROUP_NAME, "", asList(parameterInGroup),
                                                             emptyList(), false, null, null, emptySet());

    errorMappingsParameter = createParameterModel(ERROR_MAPPINGS_PARAMETER_NAME, false, BaseTypeBuilder.create(JAVA).arrayType()
        .of(TYPE_LOADER.load(ErrorMapping.class)).build(), null, NOT_SUPPORTED,
                                                  BEHAVIOUR, null, emptyList());

    errorMappingsParameterGroup = new ImmutableParameterGroupModel(ERROR_MAPPINGS, "", asList(errorMappingsParameter),
                                                                   emptyList(), false, null, null, emptySet());

    componentParameterGroup = new ImmutableParameterGroupModel(DEFAULT_GROUP_NAME, "", asList(configRefParameter,
                                                                                              actingParameter,
                                                                                              providedParameter,
                                                                                              parameterRequiredForMetadata,
                                                                                              complexActingParameter,
                                                                                              providedParameterFromComplex),
                                                               emptyList(), false,
                                                               null, null, emptySet());

    configParameterGroup = new ImmutableParameterGroupModel(DEFAULT_GROUP_NAME, "",
                                                            asList(
                                                                   nameParameter,
                                                                   actingParameter,
                                                                   providedParameter,
                                                                   parameterRequiredForMetadata,
                                                                   complexActingParameter,
                                                                   providedParameterFromComplex),
                                                            emptyList(), false, null, null, emptySet());

    RequiredForMetadataModelProperty requiredForMetadataModelProperty =
        new RequiredForMetadataModelProperty(asList(PARAMETER_REQUIRED_FOR_METADATA_NAME));

    connectionProvider =
        new ImmutableConnectionProviderModel(CONNECTION_PROVIDER_NAME, "", asList(componentParameterGroup, actingParametersGroup),
                                             NONE,
                                             false, emptySet(), null,
                                             CONNECTION, Collections.singleton(requiredForMetadataModelProperty));

    operation =
        createOperationModel(OPERATION_NAME, asList(componentParameterGroup, actingParametersGroup, errorMappingsParameterGroup));
    otherOperation =
        createOperationModel(OTHER_OPERATION_NAME,
                             asList(componentParameterGroup, actingParametersGroup, errorMappingsParameterGroup));

    source = new ImmutableSourceModel(SOURCE_NAME, "", false, false, asList(componentParameterGroup, actingParametersGroup),
                                      emptyList(),
                                      null, null, empty(), empty(), empty(), false, false, false, null, null, emptySet(),
                                      PUBLIC, emptySet(), emptySet(), null);

    configuration = new ImmutableConfigurationModel(CONFIGURATION_NAME, "", asList(configParameterGroup, actingParametersGroup),
                                                    asList(operation, otherOperation), asList(connectionProvider), asList(source),
                                                    emptySet(), null, CONFIG, singleton(requiredForMetadataModelProperty));
    otherConfiguration =
        new ImmutableConfigurationModel(OTHER_CONFIGURATION_NAME, "", asList(configParameterGroup, actingParametersGroup),
                                        emptyList(), asList(connectionProvider), emptyList(),
                                        emptySet(), null, CONFIG, singleton(requiredForMetadataModelProperty));

    mockExtension = createExtensionMock();

    when(dslContext.getExtension(any())).thenReturn(of(mockExtension));
    when(dslContext.getExtensions()).thenReturn(singleton(mockExtension));

    extensions = ImmutableSet.<ExtensionModel>builder()
        .add(MuleExtensionModelProvider.getExtensionModel())
        .add(mockExtension)
        .build();

    TypeCatalog typeCatalog = DslResolvingContext.getDefault(extensions).getTypeCatalog();

    when(dslContext.getTypeCatalog()).thenReturn(typeCatalog);

    declarer = ElementDeclarer.forExtension(EXTENSION_NAME);
  }

  private ImmutableParameterModel createParameterModel(String paramName, boolean isComponentId, MetadataType type,
                                                       Object defaultValue, ExpressionSupport expressionSupport,
                                                       ParameterRole parameterRole, ValueProviderModel valueProviderModel,
                                                       List<StereotypeModel> allowedStereotypes) {
    final String targetSelector = "some.target.path";
    FieldValueProviderModel fieldValueProviderModel =
        createFieldValueProviderModel(FIELD_VALUE_PROVIDER_NAME, FIELD_VALUE_PROVIDER_ID, targetSelector);

    ActingParameterModel actingParameterModel = createActingParameterModel(COMPLEX_ACTING_PARAMETER_NAME,
                                                                           COMPLEX_ACTING_PARAMETER_NAME
                                                                               + ".innerPojoParam.stringParam");

    when(fieldValueProviderModel.getParameters()).thenReturn(singletonList(actingParameterModel));

    return new ImmutableParameterModel(paramName, "", type, false, false,
                                       false, isComponentId, expressionSupport, defaultValue,
                                       parameterRole, ParameterDslConfiguration.getDefaultInstance(), null, null,
                                       valueProviderModel,
                                       allowedStereotypes, emptySet(), null, null, singletonList(fieldValueProviderModel));
  }

  private ImmutableOperationModel createOperationModel(String operationName, List<ParameterGroupModel> paramGroups) {
    return new ImmutableOperationModel(operationName, "",
                                       paramGroups,
                                       emptyList(), null, null, true, ExecutionType.BLOCKING, false, false, false, null,
                                       emptySet(), PROCESSOR, PUBLIC, emptySet(), emptySet());
  }

  protected ExtensionModel createExtensionMock() {
    return new ImmutableExtensionModel(EXTENSION_NAME,
                                       "",
                                       "1.0",
                                       "Mulesoft",
                                       COMMUNITY,
                                       asList(configuration, otherConfiguration),
                                       asList(operation, otherOperation),
                                       asList(connectionProvider),
                                       asList(source),
                                       emptyList(),
                                       emptyList(),
                                       null,
                                       XmlDslModel.builder()
                                           .setXsdFileName("mule-mockns.xsd")
                                           .setPrefix(NAMESPACE)
                                           .setNamespace(NAMESPACE_URI)
                                           .setSchemaLocation(SCHEMA_LOCATION)
                                           .setSchemaVersion("4.0")
                                           .build(),
                                       emptySet(),
                                       emptySet(),
                                       emptySet(),
                                       emptySet(),
                                       emptySet(),
                                       emptySet(),
                                       emptySet(),
                                       emptySet(),
                                       emptySet(),
                                       emptySet());
  }

  protected ActingParameterModel createActingParameterModel(String parameterName) {
    return createActingParameterModel(parameterName, parameterName);
  }

  protected ActingParameterModel createActingParameterModel(String parameterName, String extractionExpression) {
    ActingParameterModel actingParameterModel = mock(ActingParameterModel.class);
    when(actingParameterModel.getName()).thenReturn(parameterName);
    when(actingParameterModel.getExtractionExpression()).thenReturn(extractionExpression);
    return actingParameterModel;
  }

  protected FieldValueProviderModel createFieldValueProviderModel(String providerName, String providerId, String targetSelector) {
    FieldValueProviderModel fieldValueProviderModel = mock(FieldValueProviderModel.class);
    when(fieldValueProviderModel.getProviderName()).thenReturn(providerName);
    when(fieldValueProviderModel.getProviderId()).thenReturn(providerId);
    when(fieldValueProviderModel.requiresConnection()).thenReturn(false);
    when(fieldValueProviderModel.requiresConfiguration()).thenReturn(false);
    when(fieldValueProviderModel.getTargetSelector()).thenReturn(targetSelector);
    return fieldValueProviderModel;
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

  public OperationElementDeclaration declareOperation(String operationName) {
    return declarer.newOperation(operationName)
        .withConfig(MY_CONFIG)
        .withParameterGroup(newParameterGroup()
            .withParameter(ACTING_PARAMETER_NAME, ACTING_PARAMETER_DEFAULT_VALUE)
            .withParameter(PROVIDED_PARAMETER_NAME, PROVIDED_PARAMETER_DEFAULT_VALUE)
            .withParameter(COMPLEX_ACTING_PARAMETER_NAME,
                           declareComplexActingParameter(
                                                         new ComplexActingParameter(
                                                                                    DEFAULT_COMPLEX_ACTING_PARAMETER
                                                                                        .getIntParam(),
                                                                                    DEFAULT_COMPLEX_ACTING_PARAMETER
                                                                                        .getStringParam(),
                                                                                    DEFAULT_COMPLEX_ACTING_PARAMETER
                                                                                        .getListParam(),
                                                                                    DEFAULT_COMPLEX_ACTING_PARAMETER
                                                                                        .getMapParam(),
                                                                                    DEFAULT_COMPLEX_ACTING_PARAMETER
                                                                                        .getInnerPojoParam(),
                                                                                    DEFAULT_COMPLEX_ACTING_PARAMETER
                                                                                        .getComplexListParam(),
                                                                                    DEFAULT_COMPLEX_ACTING_PARAMETER
                                                                                        .getComplexMapParam())))
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


  protected ComponentAst getComponentAst(ArtifactAst app, String location) {
    Reference<ComponentAst> componentAst = new Reference<>();
    app.recursiveStream().forEach(c -> {
      if (c.getLocation().getLocation().equals(location)) {
        componentAst.set(c);
      }
    });
    return componentAst.get();
  }

  protected Optional<ElementDeclaration> getDeclaration(ArtifactDeclaration app, String location) {
    return app.findElement(builderFromStringRepresentation(location).build());
  }


  protected ArtifactAst loadApplicationModel(ArtifactDeclaration declaration) throws Exception {
    return toArtifactast(declaration, extensions);
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

    Locator(ArtifactAst app) {
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
