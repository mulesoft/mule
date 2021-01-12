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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.api.util.ExtensionModelTestUtils.visitableMock;
import static org.mule.runtime.app.declaration.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newParameterGroup;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULE_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.FLOW_ELEMENT_IDENTIFIER;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.metadata.api.ClassTypeLoader;
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
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.ConfigurationElementDeclaration;
import org.mule.runtime.app.declaration.api.ConnectionElementDeclaration;
import org.mule.runtime.app.declaration.api.OperationElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterizedElementDeclaration;
import org.mule.runtime.app.declaration.api.fluent.ElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.ParameterSimpleValue;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.api.dsl.model.DslElementModel;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.config.api.dsl.model.metadata.DslElementBasedValueProviderCacheIdGenerator;
import org.mule.runtime.config.api.dsl.processor.ArtifactConfig;
import org.mule.runtime.config.internal.model.ApplicationModel;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.core.api.extension.MuleExtensionModelProvider;
import org.mule.runtime.core.internal.locator.ComponentLocator;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheId;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheIdGenerator;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;

public class DslModelValueProviderCacheIdGeneratorTestCase extends AbstractMuleTestCase {

  private static final Logger LOGGER = getLogger(DslModelValueProviderCacheIdGeneratorTestCase.class);

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
  protected static final String PROVIDED_PARAMETER_DEFAULT_VALUE = "providedParameter";
  protected static final String EXTENSION_NAME = "extension";
  protected static final String OPERATION_NAME = "mockOperation";
  protected static final String OTHER_OPERATION_NAME = "mockOtherOperation";
  protected static final String SOURCE_NAME = "source";
  protected static final String CONFIGURATION_NAME = "configuration";
  protected static final String OTHER_CONFIGURATION_NAME = "otherConfiguration";
  protected static final String CONNECTION_PROVIDER_NAME = "connection";
  protected static final String VALUE_PROVIDER_NAME = "valueProvider";

  private static final String MY_FLOW = "myFlow";
  private static final String MY_CONFIG = "myConfig";
  private static final String MY_CONNECTION = MY_CONFIG + "/connection"; // Not a valid location, hack to reuse helper function.
  private static final String SOURCE_LOCATION = MY_FLOW + "/source";
  private static final String OPERATION_LOCATION = MY_FLOW + "/processors/0";
  private static final String OTHER_OPERATION_LOCATION = MY_FLOW + "/processors/1";

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
  protected ParameterGroupModel parameterGroup;

  @Mock(lenient = true)
  protected ParameterModel parameterRequiredForMetadata;

  @Mock(lenient = true)
  protected DslResolvingContext dslContext;

  @Mock(lenient = true)
  protected ValueProviderModel valueProviderModel;

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

    this.defaultGroupParameterModels = asList(nameParameter, actingParameter, providedParameter, parameterRequiredForMetadata);
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
    when(configuration.getOperationModels()).thenReturn(asList(operation));
    when(configuration.getSourceModels()).thenReturn(asList(source));
    when(configuration.getConnectionProviders()).thenReturn(asList(connectionProvider));
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
    when(operation.getParameterGroupModels()).thenReturn(asList(parameterGroup, actingParametersGroup));

    visitableMock(operation, source);

    when(otherOperation.getName()).thenReturn(OTHER_OPERATION_NAME);
    when(otherOperation.getParameterGroupModels()).thenReturn(asList(parameterGroup, actingParametersGroup));

    visitableMock(otherOperation, source);

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

  private ConfigurationElementDeclaration declareConfig(ConnectionElementDeclaration connectionDeclaration, String name,
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

  private ConfigurationElementDeclaration declareOtherConfig(ConnectionElementDeclaration connectionDeclaration, String name,
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

  private ConnectionElementDeclaration declareConnection(String parameterRequiredForMetadata, String actingParameter,
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


  private ArtifactDeclaration getBaseApp() {
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

  private OperationElementDeclaration declareOperation(String operationName) {
    return declarer.newOperation(operationName)
        .withConfig(MY_CONFIG)
        .withParameterGroup(newParameterGroup()
            .withParameter(ACTING_PARAMETER_NAME, ACTING_PARAMETER_DEFAULT_VALUE)
            .withParameter(PROVIDED_PARAMETER_NAME, PROVIDED_PARAMETER_DEFAULT_VALUE)
            .getDeclaration())
        .withParameterGroup(newParameterGroup(CUSTOM_PARAMETER_GROUP_NAME)
            .withParameter(PARAMETER_IN_GROUP_NAME, PARAMETER_IN_GROUP_DEFAULT_VALUE)
            .getDeclaration())
        .getDeclaration();
  }


  private ComponentAst getComponentAst(ApplicationModel app, String location) {
    Reference<ComponentAst> componentAst = new Reference<>();
    app.getRootComponentModel().executedOnEveryInnerComponent(
                                                              c -> {
                                                                if (c.getComponentLocation().getLocation().equals(location)) {
                                                                  componentAst.set((ComponentAst) c);
                                                                }
                                                              });
    return componentAst.get();
  }


  protected ApplicationModel loadApplicationModel(ArtifactDeclaration declaration) throws Exception {
    return new ApplicationModel(new ArtifactConfig.Builder().build(),
                                declaration, extensions, emptyMap(), empty(), empty(),
                                uri -> getClass().getResourceAsStream(uri), getFeatureFlaggingService());
  }

  private Optional<ValueProviderCacheId> computeIdFor(ArtifactDeclaration appDeclaration,
                                                      String location,
                                                      String parameterName)
      throws Exception {
    ApplicationModel app = loadApplicationModel(appDeclaration);
    DslElementModelFactory dslFactory = DslElementModelFactory.getDefault(dslContext);
    Locator locator = new Locator(app);
    ComponentLocator<DslElementModel<?>> dslLocator = l -> locator.get(l).map(c -> dslFactory.create(c).orElse(null));
    ValueProviderCacheIdGenerator cacheIdGenerator = new DslElementBasedValueProviderCacheIdGenerator(dslLocator);
    ComponentAst component = getComponentAst(app, location);
    DslElementModel<?> element = DslElementModelFactory.getDefault(dslContext).create(component).get();
    return cacheIdGenerator.getIdForResolvedValues(element, parameterName);
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

  private void checkIdsAreEqual(Optional<ValueProviderCacheId> id1, Optional<ValueProviderCacheId> id2) {
    LOGGER.debug("ID1: " + id1.map(i -> collectLog(i, 0)).orElse("empty"));
    LOGGER.debug("ID2: " + id2.map(i -> collectLog(i, 0)).orElse("empty"));
    assertThat(id1, equalTo(id2));
  }

  private void checkIdsAreDifferent(Optional<ValueProviderCacheId> id1, Optional<ValueProviderCacheId> id2) {
    LOGGER.debug("ID1: " + id1.map(i -> collectLog(i, 0)).orElse("empty"));
    LOGGER.debug("ID2: " + id2.map(i -> collectLog(i, 0)).orElse("empty"));
    assertThat(id1, not(equalTo(id2)));
  }

  private Optional<ParameterizedElementDeclaration> getParameterElementDeclaration(ArtifactDeclaration artifactDeclaration,
                                                                                   String location) {
    AtomicBoolean isConnection = new AtomicBoolean(false);
    if (location.endsWith("/connection")) {
      isConnection.set(true);
      location = location.split("/connection")[0];
    }
    return artifactDeclaration.<ParameterizedElementDeclaration>findElement(builderFromStringRepresentation(location).build())
        .map(d -> isConnection.get() ? ((ConfigurationElementDeclaration) d).getConnection().orElse(null) : d);
  }

  private void modifyParameter(ArtifactDeclaration artifactDeclaration, String ownerLocation, String parameterName,
                               Consumer<ParameterElementDeclaration> parameterConsumer) {
    getParameterElementDeclaration(artifactDeclaration, ownerLocation)
        .map(
             owner -> owner.getParameterGroups()
                 .stream()
                 .flatMap(pg -> pg.getParameters().stream())
                 .filter(p -> p.getName().equals(parameterName))
                 .findAny()
                 .map(fp -> {
                   parameterConsumer.accept(fp);
                   return EMPTY; // Needed to avoid exception
                 })
                 .orElseThrow(() -> new RuntimeException("Could not find parameter to modify")))
        .orElseThrow(() -> new RuntimeException("Location not found"));
  }


  @Test
  public void idForParameterWithNoProviderInConfig() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    assertThat(computeIdFor(app, MY_CONFIG, ACTING_PARAMETER_NAME).isPresent(), is(false));
  }

  @Test
  public void idForParameterWithNoProviderInSource() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    assertThat(computeIdFor(app, SOURCE_LOCATION, ACTING_PARAMETER_NAME).isPresent(), is(false));
  }

  @Test
  public void idForParameterWithNoProviderInOperation() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    assertThat(computeIdFor(app, OPERATION_LOCATION, ACTING_PARAMETER_NAME).isPresent(), is(false));
  }

  @Test
  public void idForConfigNoChanges() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> configId = computeIdFor(app, MY_CONFIG, PROVIDED_PARAMETER_NAME);
    assertThat(configId.isPresent(), is(true));
    checkIdsAreEqual(configId, computeIdFor(app, MY_CONFIG, PROVIDED_PARAMETER_NAME));
  }



  @Test
  public void idForConfigChangingNotActingParameters() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> configId = computeIdFor(app, MY_CONFIG, PROVIDED_PARAMETER_NAME);
    assertThat(configId.isPresent(), is(true));
    modifyParameter(app, MY_CONFIG, PARAMETER_REQUIRED_FOR_METADATA_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreEqual(configId, computeIdFor(app, MY_CONFIG, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfigChangingActingParameter() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> configId = computeIdFor(app, MY_CONFIG, PROVIDED_PARAMETER_NAME);
    assertThat(configId.isPresent(), is(true));
    modifyParameter(app, MY_CONFIG, ACTING_PARAMETER_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreDifferent(configId, computeIdFor(app, MY_CONFIG, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfigChangingActingParameterInGroup() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> configId = computeIdFor(app, MY_CONFIG, PROVIDED_PARAMETER_NAME);
    assertThat(configId.isPresent(), is(true));
    modifyParameter(app, MY_CONFIG, PARAMETER_IN_GROUP_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreDifferent(configId, computeIdFor(app, MY_CONFIG, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfiglessAndConnectionlessOperationNoChanges() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> opId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));
    checkIdsAreEqual(opId, computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfiglessAndConnectionlessOperationChangingActingParameter() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> opId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));
    modifyParameter(app, OPERATION_LOCATION, ACTING_PARAMETER_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreDifferent(opId, computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfiglessAndConnectionlessOperationChangingActingParameterInGroup() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> opId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));
    modifyParameter(app, OPERATION_LOCATION, PARAMETER_IN_GROUP_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreDifferent(opId, computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfiglessAndConnectionlessOperationChangesInConfig() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> opId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));
    modifyParameter(app, MY_CONFIG, PARAMETER_REQUIRED_FOR_METADATA_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    modifyParameter(app, MY_CONFIG, ACTING_PARAMETER_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreEqual(opId, computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfiglessAndConnectionlessOperationChangesInConnection() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> opId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));
    modifyParameter(app, MY_CONNECTION, PARAMETER_REQUIRED_FOR_METADATA_NAME,
                    p -> p.setValue(ParameterSimpleValue.of("newValue")));
    modifyParameter(app, MY_CONNECTION, ACTING_PARAMETER_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreEqual(opId, computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME));
  }


  @Test
  public void idForConfiglessAndConnectionlessSourceNoChanges() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> sourceId = computeIdFor(app, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(sourceId.isPresent(), is(true));
    checkIdsAreEqual(sourceId, computeIdFor(app, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfiglessAndConnectionlessSourceChangingActingParameter() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> sourceId = computeIdFor(app, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(sourceId.isPresent(), is(true));
    modifyParameter(app, SOURCE_LOCATION, ACTING_PARAMETER_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreDifferent(sourceId, computeIdFor(app, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfiglessAndConnectionlessSourceChangingActingParameterInGroup() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> sourceId = computeIdFor(app, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(sourceId.isPresent(), is(true));
    modifyParameter(app, SOURCE_LOCATION, PARAMETER_IN_GROUP_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreDifferent(sourceId, computeIdFor(app, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfiglessAndConnectionlessSourceChangesInConfig() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> sourceId = computeIdFor(app, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(sourceId.isPresent(), is(true));
    modifyParameter(app, MY_CONFIG, PARAMETER_REQUIRED_FOR_METADATA_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    modifyParameter(app, MY_CONFIG, ACTING_PARAMETER_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreEqual(sourceId, computeIdFor(app, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfiglessAndConnectionlessSourceChangesInConnection() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> sourceId = computeIdFor(app, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(sourceId.isPresent(), is(true));
    modifyParameter(app, MY_CONNECTION, PARAMETER_REQUIRED_FOR_METADATA_NAME,
                    p -> p.setValue(ParameterSimpleValue.of("newValue")));
    modifyParameter(app, MY_CONNECTION, ACTING_PARAMETER_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreEqual(sourceId, computeIdFor(app, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfigAwareOperationChangesInConfigNotRequiredForMetadata() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    when(valueProviderModel.requiresConfiguration()).thenReturn(true);
    Optional<ValueProviderCacheId> opId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));
    modifyParameter(app, MY_CONFIG, ACTING_PARAMETER_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreEqual(opId, computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfigAwareOperationChangesInConfigRequiredForMetadata() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    when(valueProviderModel.requiresConfiguration()).thenReturn(true);
    Optional<ValueProviderCacheId> opId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));
    modifyParameter(app, MY_CONFIG, PARAMETER_REQUIRED_FOR_METADATA_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreDifferent(opId, computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConnectionAwareOperationChangesInConnectionNotRequiredForMetadata() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    when(valueProviderModel.requiresConnection()).thenReturn(true);
    Optional<ValueProviderCacheId> opId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));
    modifyParameter(app, MY_CONNECTION, ACTING_PARAMETER_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreEqual(opId, computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConnectionAwareOperationChangesInConnectionRequiredForMetadata() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    when(valueProviderModel.requiresConnection()).thenReturn(true);
    Optional<ValueProviderCacheId> opId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));
    modifyParameter(app, MY_CONNECTION, PARAMETER_REQUIRED_FOR_METADATA_NAME,
                    p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreDifferent(opId, computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfigAwareSourceChangesInConfigNotRequiredForMetadata() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    when(valueProviderModel.requiresConfiguration()).thenReturn(true);
    Optional<ValueProviderCacheId> sourceId = computeIdFor(app, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(sourceId.isPresent(), is(true));
    modifyParameter(app, MY_CONFIG, ACTING_PARAMETER_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreEqual(sourceId, computeIdFor(app, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfigAwareSourceChangesInConfigRequiredForMetadata() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    when(valueProviderModel.requiresConfiguration()).thenReturn(true);
    Optional<ValueProviderCacheId> sourceId = computeIdFor(app, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(sourceId.isPresent(), is(true));
    modifyParameter(app, MY_CONFIG, PARAMETER_REQUIRED_FOR_METADATA_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreDifferent(sourceId, computeIdFor(app, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConnectionAwareSourceChangesInConnectionNotRequiredForMetadata() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    when(valueProviderModel.requiresConnection()).thenReturn(true);
    Optional<ValueProviderCacheId> sourceId = computeIdFor(app, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(sourceId.isPresent(), is(true));
    modifyParameter(app, MY_CONNECTION, ACTING_PARAMETER_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreEqual(sourceId, computeIdFor(app, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConnectionAwareSourceChangesInConnectionRequiredForMetadata() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    when(valueProviderModel.requiresConnection()).thenReturn(true);
    Optional<ValueProviderCacheId> sourceId = computeIdFor(app, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(sourceId.isPresent(), is(true));
    modifyParameter(app, MY_CONNECTION, PARAMETER_REQUIRED_FOR_METADATA_NAME,
                    p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreDifferent(sourceId, computeIdFor(app, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void equalConfigsWithDifferentNameGetSameHash() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    ConfigurationElementDeclaration config = (ConfigurationElementDeclaration) app.getGlobalElements().get(0);
    app.addGlobalElement(declareConfig(config.getConnection().get(), "newName",
                                       PARAMETER_REQUIRED_FOR_METADATA_DEFAULT_VALUE,
                                       ACTING_PARAMETER_DEFAULT_VALUE,
                                       PROVIDED_PARAMETER_DEFAULT_VALUE,
                                       PARAMETER_IN_GROUP_DEFAULT_VALUE));
    Optional<ValueProviderCacheId> config1Id = computeIdFor(app, MY_CONFIG, PROVIDED_PARAMETER_NAME);
    Optional<ValueProviderCacheId> config2Id = computeIdFor(app, "newName", PROVIDED_PARAMETER_NAME);
    checkIdsAreEqual(config1Id, config2Id);
  }

  @Test
  public void differentConfigsWithSameParameterGetDifferentHash() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    ConfigurationElementDeclaration config = (ConfigurationElementDeclaration) app.getGlobalElements().get(0);
    app.addGlobalElement(declareOtherConfig(config.getConnection().get(), "newName",
                                            PARAMETER_REQUIRED_FOR_METADATA_DEFAULT_VALUE,
                                            ACTING_PARAMETER_DEFAULT_VALUE,
                                            PROVIDED_PARAMETER_DEFAULT_VALUE,
                                            PARAMETER_IN_GROUP_DEFAULT_VALUE));
    Optional<ValueProviderCacheId> config1Id = computeIdFor(app, MY_CONFIG, PROVIDED_PARAMETER_NAME);
    Optional<ValueProviderCacheId> config2Id = computeIdFor(app, "newName", PROVIDED_PARAMETER_NAME);
    checkIdsAreDifferent(config1Id, config2Id);
  }

  @Test
  public void differentValueProviderNameGetsDifferentHash() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    when(valueProviderModel.requiresConnection()).thenReturn(true);
    when(valueProviderModel.requiresConfiguration()).thenReturn(true);
    Optional<ValueProviderCacheId> opId1 = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    when(valueProviderModel.getProviderName()).thenReturn("newValueProviderName");
    Optional<ValueProviderCacheId> opId2 = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    checkIdsAreDifferent(opId1, opId2);
  }

  @Test
  public void differentOperationsWithSameParametersGetsDifferentHash() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> opId1 = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    Optional<ValueProviderCacheId> opId2 = computeIdFor(app, OTHER_OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    checkIdsAreDifferent(opId1, opId2);
  }

  private static class Locator implements ComponentLocator<ComponentAst> {

    private final Map<Location, ComponentModel> components = new HashMap<>();

    Locator(ApplicationModel app) {
      app.getRootComponentModel().getInnerComponents().forEach(this::addComponent);
    }

    @Override
    public Optional<ComponentAst> get(Location location) {
      return Optional.ofNullable(components.get(location)).map(cm -> (ComponentAst) cm);
    }

    private Location getLocation(ComponentModel component) {
      return Location.builderFromStringRepresentation(component.getComponentLocation().getLocation()).build();
    }

    private void addComponent(ComponentModel component) {
      components.put(getLocation(component), component);
      component.getInnerComponents().forEach(this::addComponent);
    }
  }

}
