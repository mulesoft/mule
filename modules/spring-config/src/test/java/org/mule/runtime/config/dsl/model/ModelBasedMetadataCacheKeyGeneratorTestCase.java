/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newListValue;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newParameterGroup;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULE_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.FLOW_ELEMENT_IDENTIFIER;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.PartialTypeKeysResolver;
import org.mule.runtime.api.metadata.resolving.QueryEntityResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.app.declaration.api.ConfigurationElementDeclaration;
import org.mule.runtime.app.declaration.api.ConstructElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterGroupElementDeclaration;
import org.mule.runtime.app.declaration.api.fluent.ElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.ParameterListValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterObjectValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterSimpleValue;
import org.mule.runtime.config.api.dsl.model.metadata.ModelBasedMetadataCacheIdGeneratorFactory;
import org.mule.runtime.config.api.dsl.processor.ArtifactConfig;
import org.mule.runtime.config.internal.model.ApplicationModel;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.core.api.extension.MuleExtensionModelProvider;
import org.mule.runtime.core.internal.metadata.NullMetadataResolverFactory;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheId;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheIdGenerator;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheIdGeneratorFactory;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;
import org.mule.runtime.extension.api.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.property.MetadataKeyPartModelProperty;
import org.mule.runtime.extension.api.property.RequiredForMetadataModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MetadataResolverFactoryModelProperty;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.creation.MockSettingsImpl;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class ModelBasedMetadataCacheKeyGeneratorTestCase extends AbstractDslModelTestCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(ModelBasedMetadataCacheKeyGeneratorTestCase.class);
  private static final String MY_FLOW = "myFlow";
  private static final String MY_CONFIG = "myConfig";
  private static final String METADATA_KEY_PART_1 = "partOne";
  private static final String METADATA_KEY_PART_2 = "partTwo";
  private static final String METADATA_KEY_PART_3 = "partThree";
  private static final String METADATA_KEY_GROUP = "Key Group";
  private static final String CATEGORY_NAME = "category";
  private static final String OPERATION_LOCATION = MY_FLOW + "/processors/0";
  public static final String MY_GLOBAL_TEMPLATE = "myGlobalTemplate";

  private Set<ExtensionModel> extensions;
  private DslResolvingContext dslResolvingContext;
  private ElementDeclarer declarer;

  @Before
  public void setUp() throws Exception {
    extensions = ImmutableSet.<ExtensionModel>builder()
        .add(MuleExtensionModelProvider.getExtensionModel())
        .add(mockExtension)
        .build();

    dslResolvingContext = DslResolvingContext.getDefault(extensions);
    declarer = ElementDeclarer.forExtension(EXTENSION_NAME);
    mockSimpleMetadataKeyId(operation);
  }

  @Test
  public void idempotentHashCalculation() throws Exception {
    ApplicationModel applicationModel = loadApplicationModel(getBaseApp());
    Map<String, MetadataCacheId> hashByLocation = new HashMap<>();

    MetadataCacheIdGenerator<ComponentConfiguration> generator = createGenerator(applicationModel);

    applicationModel.getRootComponentModel().getInnerComponents()
        .forEach(component -> hashByLocation.put(component.getComponentLocation().getLocation(),
                                                 generator.getIdForComponentMetadata(component.getConfiguration()).get()));

    LOGGER.debug(hashByLocation.toString());

    ApplicationModel reload = loadApplicationModel(getBaseApp());
    MetadataCacheIdGenerator<ComponentConfiguration> otherGenerator = createGenerator(reload);

    reload.getRootComponentModel().getInnerComponents()
        .forEach(component -> {
          String location = component.getComponentLocation().getLocation();
          MetadataCacheId previousHash = hashByLocation.get(location);
          assertThat(previousHash, is(otherGenerator.getIdForComponentMetadata(component.getConfiguration()).get()));
        });
  }

  @Test
  public void configurationParameterModifiesHash() throws Exception {
    ArtifactDeclaration declaration = getBaseApp();
    MetadataCacheId cacheId = getIdForComponent(declaration);
    LOGGER.debug(cacheId.toString());

    ((ConfigurationElementDeclaration) declaration.getGlobalElements().get(0)).getParameterGroups().get(0)
        .getParameter(BEHAVIOUR_NAME).get().setValue(ParameterSimpleValue.of("otherText"));

    MetadataCacheId otherKeyParts = getIdForComponent(declaration);
    LOGGER.debug(otherKeyParts.toString());
    assertThat(cacheId, not(otherKeyParts));
  }

  @Test
  public void configurationParameterModifiesGlobalHash() throws Exception {
    ArtifactDeclaration declaration = getBaseApp();
    MetadataCacheId cacheId = getGlobalId(declaration);
    LOGGER.debug(cacheId.toString());

    ((ConfigurationElementDeclaration) declaration.getGlobalElements().get(0)).getParameterGroups().get(0)
        .getParameter(BEHAVIOUR_NAME).get().setValue(ParameterSimpleValue.of("otherText"));

    MetadataCacheId otherKeyParts = getGlobalId(declaration);
    LOGGER.debug(otherKeyParts.toString());
    assertThat(cacheId, not(otherKeyParts));
  }

  @Test
  public void operationParameterDoesNotModifyHash() throws Exception {
    ArtifactDeclaration declaration = getBaseApp();
    MetadataCacheId keyParts = getIdForComponent(declaration);
    LOGGER.debug(keyParts.toString());

    ComponentElementDeclaration operationDeclaration = ((ConstructElementDeclaration) declaration.getGlobalElements().get(1))
        .getComponents().get(0);
    operationDeclaration.getParameterGroups().get(0)
        .getParameter(CONTENT_NAME).get().setValue(ParameterSimpleValue.of("otherValue"));

    operationDeclaration.getParameterGroups().get(0)
        .addParameter(newParam(BEHAVIOUR_NAME, "notKey"));

    MetadataCacheId otherKeyParts = getIdForComponent(declaration);
    LOGGER.debug(otherKeyParts.toString());
    assertThat(keyParts, is(otherKeyParts));
  }

  @Test
  public void operationParameterDoesNotModifyGlobal() throws Exception {
    ArtifactDeclaration declaration = getBaseApp();
    MetadataCacheId keyParts = getGlobalId(declaration);
    LOGGER.debug(keyParts.toString());

    ComponentElementDeclaration operationDeclaration = ((ConstructElementDeclaration) declaration.getGlobalElements().get(1))
        .getComponents().get(0);
    operationDeclaration.getParameterGroups().get(0)
        .getParameter(CONTENT_NAME).get().setValue(ParameterSimpleValue.of("otherValue"));
    operationDeclaration.getParameterGroups().get(0)
        .addParameter(newParam(BEHAVIOUR_NAME, "notKey"));

    MetadataCacheId otherKeyParts = getGlobalId(declaration);
    LOGGER.debug(otherKeyParts.toString());
    assertThat(keyParts, is(otherKeyParts));
  }

  @Test
  public void metadataKeyModifiesHash() throws Exception {
    MetadataCacheId keyParts = getIdForComponent(getBaseApp());
    LOGGER.debug(keyParts.toString());

    ArtifactDeclaration declaration = getBaseApp();
    ComponentElementDeclaration operationDeclaration = ((ConstructElementDeclaration) declaration.getGlobalElements().get(1))
        .getComponents().get(0);

    ParameterElementDeclaration metadataKeyPartParam = newParam(METADATA_KEY_PART_1, "User");
    operationDeclaration.getParameterGroups().get(0).addParameter(metadataKeyPartParam);

    MetadataCacheId otherKeyParts = getIdForComponent(declaration);
    LOGGER.debug(otherKeyParts.toString());

    metadataKeyPartParam.setValue(ParameterSimpleValue.of("Document"));

    MetadataCacheId finalKeyParts = getIdForComponent(declaration);
    LOGGER.debug(finalKeyParts.toString());

    assertThat(otherKeyParts, not(keyParts));
    assertThat(finalKeyParts, not(keyParts));
    assertThat(finalKeyParts, not(otherKeyParts));
  }

  @Test
  public void metadataCategoryModifiesGlobalHash() throws Exception {
    ArtifactDeclaration declaration = getBaseApp();
    ComponentElementDeclaration operationDeclaration = ((ConstructElementDeclaration) declaration.getGlobalElements().get(1))
        .getComponents().get(0);

    ParameterElementDeclaration metadataKeyPartParam = newParam(METADATA_KEY_PART_1, "User");
    operationDeclaration.getParameterGroups().get(0).addParameter(metadataKeyPartParam);

    MetadataCacheId id = getGlobalId(declaration);
    LOGGER.debug(id.toString());

    when(operation.getModelProperty(MetadataKeyIdModelProperty.class))
        .thenReturn(of(new MetadataKeyIdModelProperty(TYPE_LOADER.load(String.class),
                                                      METADATA_KEY_PART_1,
                                                      "OTHER_CATEGORY")));

    MetadataCacheId otherId = getGlobalId(declaration);
    LOGGER.debug(id.toString());

    assertThat(id, not(otherId));
  }

  @Test
  public void allParametersAsRequiredForMetadataDoesNotModifyHash() throws Exception {

    mockRequiredForMetadataModelProperty(configuration, null);
    mockRequiredForMetadataModelProperty(connectionProvider, null);

    MetadataCacheId keyParts = getIdForComponent(getBaseApp());
    LOGGER.debug(keyParts.toString());

    List<String> parameterNames = parameterGroupModel.getParameterModels().stream()
        .map(parameterModel -> parameterModel.getName()).collect(Collectors.toList());

    mockRequiredForMetadataModelProperty(configuration, parameterNames);
    mockRequiredForMetadataModelProperty(connectionProvider, parameterNames);

    MetadataCacheId otherKeyParts = getIdForComponent(getBaseApp());
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, is(otherKeyParts));
  }

  @Test
  public void allParametersInConnectionAsRequiredForMetadataDoesNotModifyHash() throws Exception {

    mockRequiredForMetadataModelProperty(configuration, null);
    mockRequiredForMetadataModelProperty(connectionProvider, null);

    MetadataCacheId keyParts = getIdForComponent(getBaseApp());
    LOGGER.debug(keyParts.toString());

    List<String> parameterNames = parameterGroupModel.getParameterModels().stream()
        .map(parameterModel -> parameterModel.getName()).collect(Collectors.toList());

    mockRequiredForMetadataModelProperty(configuration, null);
    mockRequiredForMetadataModelProperty(connectionProvider, parameterNames);

    MetadataCacheId otherKeyParts = getIdForComponent(getBaseApp());
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, is(otherKeyParts));
  }

  @Test
  public void configurationParametersAsRequireForMetadataModifiesHash() throws Exception {

    mockRequiredForMetadataModelProperty(configuration, null);
    mockRequiredForMetadataModelProperty(connectionProvider, null);

    MetadataCacheId keyParts = getIdForComponent(getBaseApp());
    LOGGER.debug(keyParts.toString());

    mockRequiredForMetadataModelProperty(configuration, asList(behaviourParameter.getName()));
    mockRequiredForMetadataModelProperty(connectionProvider, null);

    MetadataCacheId otherKeyParts = getIdForComponent(getBaseApp());
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));

  }

  @Test
  public void connectionParametersAsRequireForMetadataModifiesHash() throws Exception {

    mockRequiredForMetadataModelProperty(configuration, null);
    mockRequiredForMetadataModelProperty(connectionProvider, null);

    MetadataCacheId keyParts = getIdForComponent(getBaseApp());
    LOGGER.debug(keyParts.toString());

    mockRequiredForMetadataModelProperty(configuration, null);
    mockRequiredForMetadataModelProperty(connectionProvider, asList(behaviourParameter.getName()));

    MetadataCacheId otherKeyParts = getIdForComponent(getBaseApp());
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));

  }

  @Test
  public void differencesInRequiredParametersForMetadataYieldsDifferentHashes() throws Exception {

    mockRequiredForMetadataModelProperty(configuration, null);
    mockRequiredForMetadataModelProperty(connectionProvider, asList(contentParameter.getName()));

    MetadataCacheId keyParts = getIdForComponent(getBaseApp());
    LOGGER.debug(keyParts.toString());

    mockRequiredForMetadataModelProperty(configuration, null);
    mockRequiredForMetadataModelProperty(connectionProvider, asList(behaviourParameter.getName()));

    MetadataCacheId otherKeyParts = getIdForComponent(getBaseApp());
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));

  }

  @Test
  public void metadataKeyDoesNotModifyKeyHash() throws Exception {
    MetadataCacheId keyParts = getKeyHash(getBaseApp(), OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    ArtifactDeclaration declaration = getBaseApp();
    ComponentElementDeclaration operationDeclaration = ((ConstructElementDeclaration) declaration.getGlobalElements().get(1))
        .getComponents().get(0);

    ParameterElementDeclaration metadataKeyPartParam = newParam(METADATA_KEY_PART_1, "User");
    operationDeclaration.getParameterGroups().get(0).addParameter(metadataKeyPartParam);

    MetadataCacheId otherKeyParts = getKeyHash(declaration, OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    metadataKeyPartParam.setValue(ParameterSimpleValue.of("Document"));

    MetadataCacheId finalKeyParts = getKeyHash(declaration, OPERATION_LOCATION);
    LOGGER.debug(finalKeyParts.toString());

    assertThat(otherKeyParts, is(keyParts));
    assertThat(finalKeyParts, is(keyParts));
    assertThat(finalKeyParts, is(otherKeyParts));
  }

  @Test
  public void multiLevelMetadataKeyModifiesHash() throws Exception {
    mockMultiLevelMetadataKeyId(operation);

    ArtifactDeclaration declaration = getBaseApp();
    ComponentElementDeclaration operationDeclaration = ((ConstructElementDeclaration) declaration.getGlobalElements().get(1))
        .getComponents().get(0);

    ParameterGroupElementDeclaration keyGroup = new ParameterGroupElementDeclaration(METADATA_KEY_GROUP);
    operationDeclaration.addParameterGroup(keyGroup);
    keyGroup.addParameter(newParam(METADATA_KEY_PART_1, "localhost"));
    keyGroup.addParameter(newParam(METADATA_KEY_PART_2, "8080"));

    MetadataCacheId twoLevelParts = getIdForComponent(declaration);
    LOGGER.debug(twoLevelParts.toString());

    keyGroup.addParameter(newParam(METADATA_KEY_PART_3, "/api"));

    MetadataCacheId otherKeyParts = getIdForComponent(declaration);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(otherKeyParts, not(twoLevelParts));
  }

  @Test
  public void multiLevelPartValueModifiesHash() throws Exception {
    mockMultiLevelMetadataKeyId(operation);

    ArtifactDeclaration declaration = getBaseApp();
    ComponentElementDeclaration operationDeclaration = ((ConstructElementDeclaration) declaration.getGlobalElements().get(1))
        .getComponents().get(0);

    ParameterGroupElementDeclaration keyGroup = new ParameterGroupElementDeclaration(METADATA_KEY_GROUP);
    operationDeclaration.addParameterGroup(keyGroup);

    keyGroup.addParameter(newParam(METADATA_KEY_PART_1, "localhost"));

    ParameterElementDeclaration partTwo = newParam(METADATA_KEY_PART_2, "8080");
    keyGroup.addParameter(partTwo);

    keyGroup.addParameter(newParam(METADATA_KEY_PART_3, "/api"));

    MetadataCacheId original = getIdForComponent(declaration);
    LOGGER.debug(original.toString());

    partTwo.setValue(ParameterSimpleValue.of("6666"));
    MetadataCacheId newHash = getIdForComponent(declaration);
    LOGGER.debug(newHash.toString());
    LOGGER.debug(newHash.toString());

    assertThat(original, not(newHash));
  }

  @Test
  public void multiLevelPartValueDoesNotModifyHashForKeys() throws Exception {
    mockMultiLevelMetadataKeyId(operation);

    ArtifactDeclaration declaration = getBaseApp();
    ComponentElementDeclaration operationDeclaration = ((ConstructElementDeclaration) declaration.getGlobalElements().get(1))
        .getComponents().get(0);

    ParameterGroupElementDeclaration keyGroup = new ParameterGroupElementDeclaration(METADATA_KEY_GROUP);
    operationDeclaration.addParameterGroup(keyGroup);

    keyGroup.addParameter(newParam(METADATA_KEY_PART_1, "localhost"));

    ParameterElementDeclaration partTwo = newParam(METADATA_KEY_PART_2, "8080");
    keyGroup.addParameter(partTwo);

    keyGroup.addParameter(newParam(METADATA_KEY_PART_3, "/api"));

    MetadataCacheId original = getKeyHash(declaration, OPERATION_LOCATION);
    LOGGER.debug(original.toString());

    partTwo.setValue(ParameterSimpleValue.of("6666"));
    MetadataCacheId newHash = getKeyHash(declaration, OPERATION_LOCATION);
    LOGGER.debug(newHash.toString());

    assertThat(original, is(newHash));
  }

  @Test
  public void multiLevelPartValueDoesNotModifyGlobalId() throws Exception {
    mockMultiLevelMetadataKeyId(operation);

    ArtifactDeclaration declaration = getBaseApp();
    ComponentElementDeclaration operationDeclaration = ((ConstructElementDeclaration) declaration.getGlobalElements().get(1))
        .getComponents().get(0);

    ParameterGroupElementDeclaration keyGroup = new ParameterGroupElementDeclaration(METADATA_KEY_GROUP);
    operationDeclaration.addParameterGroup(keyGroup);
    keyGroup.addParameter(newParam(METADATA_KEY_PART_1, "localhost"));
    ParameterElementDeclaration partTwo = newParam(METADATA_KEY_PART_2, "8080");
    keyGroup.addParameter(partTwo);
    keyGroup.addParameter(newParam(METADATA_KEY_PART_3, "/api"));

    MetadataCacheId original = getGlobalId(declaration);
    LOGGER.debug(original.toString());

    partTwo.setValue(ParameterSimpleValue.of("6666"));
    MetadataCacheId newHash = getGlobalId(declaration);
    LOGGER.debug(newHash.toString());

    assertThat(original, is(newHash));
  }

  @Test
  public void partialFetchingMultiLevelPartValueModifiesHashForKeys() throws Exception {
    mockMultiLevelMetadataKeyId(operation);
    setPartialFetchingMock(operation);

    ArtifactDeclaration declaration = getBaseApp();
    ComponentElementDeclaration operationDeclaration = ((ConstructElementDeclaration) declaration.getGlobalElements().get(1))
        .getComponents().get(0);

    ParameterGroupElementDeclaration keyGroup = new ParameterGroupElementDeclaration(METADATA_KEY_GROUP);
    operationDeclaration.addParameterGroup(keyGroup);

    keyGroup.addParameter(newParam(METADATA_KEY_PART_1, "localhost"));

    ParameterElementDeclaration partTwo = newParam(METADATA_KEY_PART_2, "8080");
    keyGroup.addParameter(partTwo);

    keyGroup.addParameter(newParam(METADATA_KEY_PART_3, "/api"));

    MetadataCacheId original = getKeyHash(declaration, OPERATION_LOCATION);
    LOGGER.debug(original.toString());

    partTwo.setValue(ParameterSimpleValue.of("6666"));
    MetadataCacheId newHash = getKeyHash(declaration, OPERATION_LOCATION);
    LOGGER.debug(newHash.toString());

    assertThat(original, not(newHash));
  }

  private void mockRequiredForMetadataModelProperty(EnrichableModel model, List<String> parameterNames) {
    if (parameterNames == null) {
      when(model.getModelProperty(RequiredForMetadataModelProperty.class))
          .thenReturn(empty());
    } else {
      RequiredForMetadataModelProperty requiredForMetadataModelProperty = new RequiredForMetadataModelProperty(parameterNames);

      when(model.getModelProperty(RequiredForMetadataModelProperty.class))
          .thenReturn(of(requiredForMetadataModelProperty));
    }
  }

  private void setPartialFetchingMock(OperationModel operation) {
    when(operation.getModelProperty(MetadataResolverFactoryModelProperty.class))
        .thenReturn(of(new MetadataResolverFactoryModelProperty(() -> new MetadataResolverFactory() {

          @Override
          public TypeKeysResolver getKeyResolver() {
            return mock(PartialTypeKeysResolver.class);
          }

          @Override
          public <T> InputTypeResolver<T> getInputResolver(String parameterName) {
            return mock(InputTypeResolver.class);
          }

          @Override
          public Collection<InputTypeResolver> getInputResolvers() {
            return Collections.emptyList();
          }

          @Override
          public <T> OutputTypeResolver<T> getOutputResolver() {
            return mock(OutputTypeResolver.class);
          }

          @Override
          public <T> AttributesTypeResolver<T> getOutputAttributesResolver() {
            return mock(AttributesTypeResolver.class);
          }

          @Override
          public QueryEntityResolver getQueryEntityResolver() {
            return mock(QueryEntityResolver.class);
          }
        })));
  }


  private MetadataCacheId getIdForComponent(ArtifactDeclaration declaration) throws Exception {
    ApplicationModel app = loadApplicationModel(declaration);
    ComponentConfiguration component = new Locator(app)
        .get(Location.builderFromStringRepresentation(ModelBasedMetadataCacheKeyGeneratorTestCase.OPERATION_LOCATION).build())
        .get();
    return createGenerator(app).getIdForComponentMetadata(component).get();
  }

  private MetadataCacheId getKeyHash(ArtifactDeclaration declaration, String location) throws Exception {
    ApplicationModel app = loadApplicationModel(declaration);
    ComponentConfiguration component = new Locator(app)
        .get(Location.builderFromStringRepresentation(location).build())
        .get();
    return createGenerator(app).getIdForMetadataKeys(component).get();
  }

  private MetadataCacheId getGlobalId(ArtifactDeclaration declaration) throws Exception {
    ApplicationModel app = loadApplicationModel(declaration);
    ComponentConfiguration component = new Locator(app)
        .get(Location.builderFromStringRepresentation(ModelBasedMetadataCacheKeyGeneratorTestCase.OPERATION_LOCATION).build())
        .get();
    return createGenerator(app).getIdForGlobalMetadata(component).get();
  }

  private ParameterElementDeclaration newParam(String name, String value) {
    ParameterElementDeclaration param = new ParameterElementDeclaration(name);
    param.setValue(ParameterSimpleValue.of(value));
    return param;
  }

  private ArtifactDeclaration getBaseApp() {
    return ElementDeclarer.newArtifact()
        .withGlobalElement(declarer.newConfiguration(CONFIGURATION_NAME)
            .withRefName(MY_CONFIG)
            .withParameterGroup(newParameterGroup()
                .withParameter(CONTENT_NAME, CONTENT_VALUE)
                .withParameter(BEHAVIOUR_NAME, BEHAVIOUR_VALUE)
                .withParameter(LIST_NAME, newListValue().withValue(ITEM_VALUE).build())
                .getDeclaration())
            .withConnection(declarer.newConnection(CONNECTION_PROVIDER_NAME)
                .withParameterGroup(newParameterGroup()
                    .withParameter(CONTENT_NAME, CONTENT_VALUE)
                    .withParameter(BEHAVIOUR_NAME, BEHAVIOUR_VALUE)
                    .withParameter(LIST_NAME,
                                   newListValue().withValue(ITEM_VALUE).build())
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(ElementDeclarer.forExtension(MULE_NAME)
            .newConstruct(FLOW_ELEMENT_IDENTIFIER)
            .withRefName(MY_FLOW)
            .withComponent(
                           declarer.newOperation(OPERATION_NAME)
                               .withConfig(MY_CONFIG)
                               .withParameterGroup(g -> g
                                   .withParameter(CONTENT_NAME, "nonKey"))
                               .getDeclaration())
            .getDeclaration())
        .withGlobalElement(declarer.newGlobalParameter("complexType")
            .withRefName(MY_GLOBAL_TEMPLATE)
            .withValue(ParameterObjectValue.builder()
                .withParameter("otherName", "simpleParam")
                .withParameter("myCamelCaseName", "someContent")
                .withParameter("numbers", ParameterListValue.builder()
                    .withValue("10")
                    .withValue("20")
                    .build())
                .build())
            .getDeclaration())
        .getDeclaration();
  }

  protected ApplicationModel loadApplicationModel(ArtifactDeclaration declaration) throws Exception {
    return new ApplicationModel(new ArtifactConfig.Builder().build(),
                                declaration, extensions, Collections.emptyMap(), Optional.empty(), Optional.empty(),
                                false, uri -> getClass().getResourceAsStream(uri));
  }

  private void mockSimpleMetadataKeyId(OperationModel model) {

    ParameterModel metadataKeyId = mockKeyPart(METADATA_KEY_PART_1, 1);

    List<ParameterModel> parameterModels = asList(contentParameter, behaviourParameter, listParameter, metadataKeyId);
    when(parameterGroupModel.getParameterModels()).thenReturn(parameterModels);
    when(parameterGroupModel.getParameter(anyString()))
        .then(invocation -> {
          String paramName = invocation.getArgument(0);
          switch (paramName) {
            case CONTENT_NAME:
              return of(contentParameter);
            case LIST_NAME:
              return of(listParameter);
            case BEHAVIOUR_NAME:
              return of(behaviourParameter);
            case METADATA_KEY_PART_1:
              return of(metadataKeyId);
          }
          return Optional.empty();
        });

    when(model.getModelProperty(MetadataKeyIdModelProperty.class))
        .thenReturn(of(new MetadataKeyIdModelProperty(TYPE_LOADER.load(String.class),
                                                      METADATA_KEY_PART_1,
                                                      CATEGORY_NAME)));

    when(operation.getModelProperty(MetadataResolverFactoryModelProperty.class)).thenReturn(Optional.empty());

    when(model.getAllParameterModels()).thenReturn(parameterModels);
  }

  private void mockMultiLevelMetadataKeyId(OperationModel operationModel) {
    ParameterModel partOne = mockKeyPart(METADATA_KEY_PART_1, 1);
    ParameterModel partTwo = mockKeyPart(METADATA_KEY_PART_2, 2);
    ParameterModel partThree = mockKeyPart(METADATA_KEY_PART_3, 3);
    List<ParameterModel> partParameterModels = asList(partOne, partTwo, partThree);

    ParameterGroupModel metadataKeyIdGroup =
        mock(ParameterGroupModel.class, new MockSettingsImpl<>().lenient().defaultAnswer(RETURNS_DEFAULTS));
    when(metadataKeyIdGroup.getName()).thenReturn(METADATA_KEY_GROUP);
    when(metadataKeyIdGroup.isShowInDsl()).thenReturn(false);
    when(metadataKeyIdGroup.getParameterModels()).thenReturn(partParameterModels);
    when(metadataKeyIdGroup.getParameter(anyString()))
        .then(invocation -> {
          String paramName = invocation.getArgument(0);
          switch (paramName) {
            case METADATA_KEY_PART_1:
              return of(partOne);
            case METADATA_KEY_PART_2:
              return of(partTwo);
            case METADATA_KEY_PART_3:
              return of(partThree);
          }
          return Optional.empty();
        });

    ObjectTypeBuilder groupType = BaseTypeBuilder.create(MetadataFormat.JAVA).objectType();
    groupType.addField().key(METADATA_KEY_PART_1).value(TYPE_LOADER.load(String.class));
    groupType.addField().key(METADATA_KEY_PART_2).value(TYPE_LOADER.load(String.class));
    groupType.addField().key(METADATA_KEY_PART_3).value(TYPE_LOADER.load(String.class));

    when(operationModel.getModelProperty(MetadataKeyIdModelProperty.class))
        .thenReturn(of(new MetadataKeyIdModelProperty(groupType.build(), METADATA_KEY_GROUP, CATEGORY_NAME)));

    when(operationModel.getModelProperty(MetadataResolverFactoryModelProperty.class))
        .thenReturn(of(new MetadataResolverFactoryModelProperty(NullMetadataResolverFactory::new)));

    when(operationModel.getParameterGroupModels()).thenReturn(Arrays.asList(parameterGroupModel, metadataKeyIdGroup));
    when(operationModel.getAllParameterModels()).thenReturn(ImmutableList.<ParameterModel>builder()
        .addAll(defaultGroupParameterModels)
        .addAll(partParameterModels)
        .build());
  }

  private ParameterModel mockKeyPart(String name, int order) {
    ParameterModel metadataKeyId = mock(ParameterModel.class);
    when(metadataKeyId.getName()).thenReturn(name);
    when(metadataKeyId.getExpressionSupport()).thenReturn(ExpressionSupport.NOT_SUPPORTED);

    when(metadataKeyId.getModelProperty(any())).then(invocation -> {
      if (invocation.getArguments()[0].equals(MetadataKeyPartModelProperty.class)) {
        return of(new MetadataKeyPartModelProperty(order));
      }
      return Optional.empty();
    });

    when(metadataKeyId.getDslConfiguration()).thenReturn(ParameterDslConfiguration.getDefaultInstance());
    when(metadataKeyId.getLayoutModel()).thenReturn(empty());
    when(metadataKeyId.getRole()).thenReturn(BEHAVIOUR);
    when(metadataKeyId.getType()).thenReturn(TYPE_LOADER.load(String.class));

    return metadataKeyId;
  }

  private MetadataCacheIdGenerator<ComponentConfiguration> createGenerator(ApplicationModel app) {
    return new ModelBasedMetadataCacheIdGeneratorFactory().create(dslResolvingContext, new Locator(app));
  }

  private static class Locator implements MetadataCacheIdGeneratorFactory.ComponentLocator<ComponentConfiguration> {

    private Map<Location, ComponentModel> components = new HashMap<>();

    Locator(ApplicationModel app) {
      app.getRootComponentModel().getInnerComponents().forEach(this::addComponent);
    }

    @Override
    public Optional<ComponentConfiguration> get(Location location) {
      return Optional.ofNullable(components.get(location).getConfiguration());
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
