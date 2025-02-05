/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model;

import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.SDK_TOOLING_SUPPORT;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.MetadataTypeResolutionStory.METADATA_CACHE;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.EnrichableModel;
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
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.serialization.ArtifactAstDeserializer;
import org.mule.runtime.ast.api.serialization.ArtifactAstSerializerProvider;
import org.mule.runtime.extension.api.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.property.MetadataKeyPartModelProperty;
import org.mule.runtime.extension.api.property.RequiredForMetadataModelProperty;
import org.mule.runtime.metadata.api.cache.MetadataCacheId;
import org.mule.runtime.metadata.internal.NullMetadataResolverFactory;
import org.mule.runtime.module.extension.internal.loader.java.property.MetadataResolverFactoryModelProperty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.Before;
import org.junit.Test;

import org.mockito.internal.creation.MockSettingsImpl;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(SDK_TOOLING_SUPPORT)
@Story(METADATA_CACHE)
public class ModelBasedMetadataCacheKeyGeneratorTestCase extends AbstractMetadataCacheIdGeneratorTestCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(ModelBasedMetadataCacheKeyGeneratorTestCase.class);
  private static final String MY_FLOW = "myFlow";
  private static final String MY_CONFIG = "myConfig";
  private static final String METADATA_KEY_PART_1 = "partOne";
  private static final String METADATA_KEY_PART_2 = "partTwo";
  private static final String METADATA_KEY_PART_3 = "partThree";
  private static final String METADATA_KEY_GROUP = "Key Group";
  private static final String CATEGORY_NAME = "category";
  private static final String OPERATION_LOCATION = MY_FLOW + "/processors/0";
  private static final String ANOTHER_OPERATION_LOCATION = MY_FLOW + "/processors/1";
  public static final String MY_GLOBAL_TEMPLATE = "myGlobalTemplate";

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    mockSimpleMetadataKeyId(operation);
    mockSimpleMetadataKeyId(anotherOperation);
  }

  @Test
  public void idempotentHashCalculation() throws Exception {
    ArtifactAst applicationModel = loadAst("idempotentHashCalculation_1.ast");
    Map<String, MetadataCacheId> hashByLocation = new HashMap<>();

    applicationModel.topLevelComponentsStream()
        .forEach(component -> {
          try {
            hashByLocation.put(component.getLocation().getLocation(),
                               getIdForComponentMetadata(applicationModel, component.getLocation().getLocation()));
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });

    LOGGER.debug(hashByLocation.toString());

    ArtifactAst reload = loadAst("idempotentHashCalculation_2.ast");

    reload.topLevelComponentsStream()
        .forEach(component -> {
          try {
            String location = component.getLocation().getLocation();
            MetadataCacheId previousHash = hashByLocation.get(location);
            assertThat(previousHash, is(getIdForComponentMetadata(reload, component.getLocation().getLocation())));
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  public void configurationParameterModifiesHash() throws Exception {
    MetadataCacheId cacheId = getIdForComponentMetadata(loadAst("configurationParameterModifiesGlobalHash_1.ast"),
                                                        OPERATION_LOCATION);
    LOGGER.debug(cacheId.toString());

    MetadataCacheId otherKeyParts =
        getIdForComponentMetadata(loadAst("configurationParameterModifiesGlobalHash_2.ast"), OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());
    assertThat(cacheId, not(otherKeyParts));
  }

  @Test
  public void configurationParameterModifiesGlobalHash() throws Exception {
    MetadataCacheId cacheId = getIdForGlobalMetadata(loadAst("configurationParameterModifiesGlobalHash_1.ast"),
                                                     OPERATION_LOCATION);
    LOGGER.debug(cacheId.toString());

    MetadataCacheId otherKeyParts =
        getIdForGlobalMetadata(loadAst("configurationParameterModifiesGlobalHash_2.ast"), OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());
    assertThat(cacheId, not(otherKeyParts));
  }

  @Test
  public void operationParameterDoesNotModifyHash() throws Exception {
    MetadataCacheId keyParts = getIdForComponentMetadata(loadAst("operationParameterDoesNotModifyHash_1.ast"),
                                                         OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    MetadataCacheId otherKeyParts =
        getIdForComponentMetadata(loadAst("operationParameterDoesNotModifyHash_2.ast"), OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());
    assertThat(keyParts, is(otherKeyParts));
  }

  @Test
  public void operationParameterDoesNotModifyGlobal() throws Exception {
    MetadataCacheId keyParts = getIdForGlobalMetadata(loadAst("operationParameterDoesNotModifyGlobal_1.ast"),
                                                      OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    MetadataCacheId otherKeyParts = getIdForGlobalMetadata(loadAst("operationParameterDoesNotModifyGlobal_2.ast"),
                                                           OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());
    assertThat(keyParts, is(otherKeyParts));
  }

  @Test
  public void metadataKeyModifiesHash() throws Exception {
    MetadataCacheId keyParts =
        getIdForComponentMetadata(loadAst("metadataKeyModifiesHash_1.ast"), OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    MetadataCacheId otherKeyParts =
        getIdForComponentMetadata(loadAst("metadataKeyModifiesHash_2.ast"), OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    MetadataCacheId finalKeyParts =
        getIdForComponentMetadata(loadAst("metadataKeyModifiesHash_3.ast"), OPERATION_LOCATION);
    LOGGER.debug(finalKeyParts.toString());

    assertThat(otherKeyParts, not(keyParts));
    assertThat(finalKeyParts, not(keyParts));
    assertThat(finalKeyParts, not(otherKeyParts));
  }

  @Test
  public void metadataCategoryModifiesGlobalHash() throws Exception {
    MetadataCacheId id = getIdForGlobalMetadata(loadAst("metadataCategoryModifiesGlobalHash_1.ast"), OPERATION_LOCATION);
    LOGGER.debug(id.toString());

    when(operation.getModelProperty(MetadataKeyIdModelProperty.class))
        .thenReturn(of(new MetadataKeyIdModelProperty(TYPE_LOADER.load(String.class),
                                                      METADATA_KEY_PART_1,
                                                      "OTHER_CATEGORY")));

    MetadataCacheId otherId = getIdForGlobalMetadata(loadAst("metadataCategoryModifiesGlobalHash_2.ast"),
                                                     OPERATION_LOCATION);
    LOGGER.debug(id.toString());

    assertThat(id, not(otherId));
  }

  @Test
  public void configurationParametersAsRequireForMetadataModifiesHash() throws Exception {
    mockRequiredForMetadataModelProperty(configuration, null);
    mockRequiredForMetadataModelProperty(connectionProvider, null);

    MetadataCacheId keyParts =
        getIdForComponentMetadata(loadAst("configurationParametersAsRequireForMetadataModifiesHash_2.ast"),
                                  OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    mockRequiredForMetadataModelProperty(configuration, asList(behaviourParameter.getName()));
    mockRequiredForMetadataModelProperty(connectionProvider, null);

    MetadataCacheId otherKeyParts =
        getIdForComponentMetadata(loadAst("configurationParametersAsRequireForMetadataModifiesHash_2.ast"),
                                  OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));

  }

  @Test
  @Issue("MULE-18601")
  public void configurationNestedParamsCountedTwiceForHash() throws Exception {
    final var app = loadAst("configurationNestedParamsCountedTwiceForHash_1.ast");
    MetadataCacheId keyParts = getIdForComponentMetadata(app, OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    assertThat(keyParts.getParts(), hasSize(3));
    assertThat(keyParts.getParts().get(0).getParts(), hasSize(2));
    assertThat(keyParts.getParts().get(0).getParts().get(0).getParts(), hasSize(0));

    final MetadataCacheId configurationPart = keyParts.getParts().get(0).getParts().get(1);
    assertThat(configurationPart.getSourceElementName().get(), is("configuration"));
    assertThat(configurationPart.getParts(), hasSize(2));
    assertThat(configurationPart.getParts().get(0).getParts(), hasSize(2));
    assertThat(configurationPart.getParts().get(0).getParts().get(0).getParts(), hasSize(0));
    assertThat(configurationPart.getParts().get(0).getParts().get(0).getParts(), hasSize(0));
    assertThat(configurationPart.getParts().get(1).getParts(), hasSize(0));
    assertThat(keyParts.getParts().get(1).getParts(), hasSize(0));
    assertThat(keyParts.getParts().get(2).getParts(), hasSize(0));

  }

  @Test
  public void connectionParametersAsRequireForMetadataModifiesHash() throws Exception {
    mockRequiredForMetadataModelProperty(configuration, null);
    mockRequiredForMetadataModelProperty(connectionProvider, null);

    final var app = loadAst("connectionParametersAsRequireForMetadataModifiesHash_1.ast");
    MetadataCacheId keyParts = getIdForComponentMetadata(app, OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    mockRequiredForMetadataModelProperty(configuration, null);
    mockRequiredForMetadataModelProperty(connectionProvider, asList(behaviourParameter.getName()));

    final var modifiedApp = loadAst("connectionParametersAsRequireForMetadataModifiesHash_2.ast");
    MetadataCacheId otherKeyParts = getIdForComponentMetadata(modifiedApp, OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void differencesInRequiredParametersForMetadataYieldsDifferentHashes() throws Exception {
    mockRequiredForMetadataModelProperty(configuration, null);
    mockRequiredForMetadataModelProperty(connectionProvider, asList(contentParameter.getName()));

    final var app = loadAst("differencesInRequiredParametersForMetadataYieldsDifferentHashes_1.ast");
    MetadataCacheId keyParts = getIdForComponentMetadata(app, OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    mockRequiredForMetadataModelProperty(configuration, null);
    mockRequiredForMetadataModelProperty(connectionProvider, asList(behaviourParameter.getName()));

    final var modifiedApp = loadAst("differencesInRequiredParametersForMetadataYieldsDifferentHashes_2.ast");
    MetadataCacheId otherKeyParts = getIdForComponentMetadata(modifiedApp, OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));

  }

  @Test
  public void metadataKeyCacheIdForConfigModelShouldIncludeConnectionParameters() throws Exception {
    final var app = loadAst("metadataKeyCacheIdForConfigModelShouldIncludeConnectionParameters_1.ast");
    MetadataCacheId keyParts = getIdForMetadataKeys(app, MY_CONFIG);
    LOGGER.debug(keyParts.toString());

    // Change the value of a connection parameter that is required for metadata
    final var modifiedApp = loadAst("metadataKeyCacheIdForConfigModelShouldIncludeConnectionParameters_2.ast");
    MetadataCacheId otherKeyParts = getIdForMetadataKeys(modifiedApp, MY_CONFIG);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(otherKeyParts, not(is(keyParts)));
  }

  @Test
  public void metadataKeyDoesNotModifyKeyHash() throws Exception {
    final var app = loadAst("metadataKeyDoesNotModifyKeyHash_1.ast");
    MetadataCacheId keyParts = getIdForMetadataKeys(app, OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    final var modifiedApp = loadAst("metadataKeyDoesNotModifyKeyHash_2.ast");
    MetadataCacheId otherKeyParts = getIdForMetadataKeys(modifiedApp, OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    final var modifiedApp2 = loadAst("metadataKeyDoesNotModifyKeyHash_3.ast");
    MetadataCacheId finalKeyParts = getIdForMetadataKeys(modifiedApp2, OPERATION_LOCATION);
    LOGGER.debug(finalKeyParts.toString());

    assertThat(otherKeyParts, is(keyParts));
    assertThat(finalKeyParts, is(keyParts));
    assertThat(finalKeyParts, is(otherKeyParts));
  }

  @Test
  public void multiLevelMetadataKeyModifiesHash() throws Exception {
    mockMultiLevelMetadataKeyId(operation);

    final var app = loadAst("multiLevelMetadataKeyModifiesHash_1.ast");
    MetadataCacheId twoLevelParts = getIdForComponentMetadata(app, OPERATION_LOCATION);
    LOGGER.debug(twoLevelParts.toString());

    final var modifiedApp = loadAst("multiLevelMetadataKeyModifiesHash_2.ast");
    MetadataCacheId otherKeyParts = getIdForComponentMetadata(modifiedApp, OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(otherKeyParts, not(twoLevelParts));
  }

  @Test
  public void multiLevelPartValueModifiesHash() throws Exception {
    mockMultiLevelMetadataKeyId(operation);

    final var app = loadAst("multiLevelPartValueModifiesHash_1.ast");
    MetadataCacheId original = getIdForComponentMetadata(app, OPERATION_LOCATION);
    LOGGER.debug(original.toString());

    final var modifiedApp = loadAst("multiLevelPartValueModifiesHash_2.ast");
    MetadataCacheId newHash = getIdForComponentMetadata(modifiedApp, OPERATION_LOCATION);
    LOGGER.debug(newHash.toString());
    LOGGER.debug(newHash.toString());

    assertThat(original, not(newHash));
  }

  @Test
  public void multiLevelPartValueDoesNotModifyHashForKeys() throws Exception {
    mockMultiLevelMetadataKeyId(operation);

    final var app = loadAst("multiLevelPartValueDoesNotModifyHashForKeys_1.ast");
    MetadataCacheId original = getIdForMetadataKeys(app, OPERATION_LOCATION);
    LOGGER.debug(original.toString());

    final var modifiedApp = loadAst("multiLevelPartValueDoesNotModifyHashForKeys_2.ast");
    MetadataCacheId newHash = getIdForMetadataKeys(modifiedApp, OPERATION_LOCATION);
    LOGGER.debug(newHash.toString());

    assertThat(original, is(newHash));
  }

  @Test
  public void multiLevelPartValueDoesNotModifyGlobalId() throws Exception {
    mockMultiLevelMetadataKeyId(operation);

    final var app = loadAst("multiLevelPartValueDoesNotModifyGlobalId_1.ast");
    MetadataCacheId original = getIdForGlobalMetadata(app, OPERATION_LOCATION);
    LOGGER.debug(original.toString());

    final var modifiedApp = loadAst("multiLevelPartValueDoesNotModifyGlobalId_2.ast");
    MetadataCacheId newHash = getIdForGlobalMetadata(modifiedApp, OPERATION_LOCATION);
    LOGGER.debug(newHash.toString());

    assertThat(original, is(newHash));
  }

  @Test
  public void metadataKeyHashIdStructure() throws Exception {
    mockMultiLevelMetadataKeyId(operation);
    setPartialFetchingMock(operation);
    mockTypeResolversInformationModelProperty(operation, CATEGORY_NAME, "outputResolver", "attributesResolver", emptyMap(),
                                              "keysResolver", true);

    final var app = loadAst("metadataKeyHashIdStructure_1.ast");
    MetadataCacheId original = getIdForMetadataKeys(app, OPERATION_LOCATION);
    LOGGER.debug(original.toString());

    assertThat(original.getParts(), hasSize(6));
    assertThat(original.getParts().get(2).getSourceElementName().get(), startsWith("category:"));
    assertThat(original.getParts().get(3).getSourceElementName().get(), startsWith("resolver:"));
    assertThat(original.getParts().get(4).getSourceElementName().get(), equalTo("metadataKey"));
    assertThat(original.getParts().get(5).getSourceElementName().get(), equalTo("metadataKeyValues"));
  }

  @Test
  public void partialFetchingMultiLevelPartValueModifiesHashForKeys() throws Exception {
    mockMultiLevelMetadataKeyId(operation);
    setPartialFetchingMock(operation);
    mockTypeResolversInformationModelProperty(operation, CATEGORY_NAME, "outputResolver", "attributesResolver", emptyMap(),
                                              "keysResolver", true);

    final var app = loadAst("partialFetchingMultiLevelPartValueModifiesHashForKeys_1.ast");
    MetadataCacheId original = getIdForMetadataKeys(app, OPERATION_LOCATION);
    LOGGER.debug(original.toString());

    final var modiifiedApp = loadAst("partialFetchingMultiLevelPartValueModifiesHashForKeys_2.ast");
    MetadataCacheId newHash = getIdForMetadataKeys(modiifiedApp, OPERATION_LOCATION);
    LOGGER.debug(newHash.toString());

    assertThat(original, not(newHash));
  }

  @Test
  public void sameSimpleMetadataKeyWithSameResolverOnDifferentOperationsGeneratesSameHashForKeys() throws Exception {
    mockTypeResolversInformationModelProperty(operation, CATEGORY_NAME, "outputResolver", "attributesResolver", emptyMap(),
                                              "keysResolver");
    mockTypeResolversInformationModelProperty(anotherOperation, CATEGORY_NAME, "outputResolver", "attributesResolver", emptyMap(),
                                              "keysResolver");

    final var app =
        loadAst("sameSimpleMetadataKeyWithSameResolverOnDifferentOperationsGeneratesSameHashForKeys_1.ast");
    MetadataCacheId operationKeysParts = getIdForMetadataKeys(app, OPERATION_LOCATION);
    LOGGER.debug(operationKeysParts.toString());

    final var modifiedApp =
        loadAst("sameSimpleMetadataKeyWithSameResolverOnDifferentOperationsGeneratesSameHashForKeys_2.ast");
    MetadataCacheId anotherOperationKeysParts = getIdForMetadataKeys(modifiedApp, ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(anotherOperationKeysParts.toString());

    assertThat(anotherOperationKeysParts, is(operationKeysParts));
  }

  @Test
  public void multilevelPartsWithSameValuesOnDifferentOperationsGeneratesSameHashForKeys() throws Exception {
    mockMultiLevelMetadataKeyId(operation);
    mockMultiLevelMetadataKeyId(anotherOperation);
    mockTypeResolversInformationModelProperty(operation, CATEGORY_NAME, "outputResolver", "attributesResolver", emptyMap(),
                                              "keysResolver");
    mockTypeResolversInformationModelProperty(anotherOperation, CATEGORY_NAME, "outputResolver", "attributesResolver", emptyMap(),
                                              "keysResolver");

    final var app = loadAst("multilevelPartsWithSameValuesOnDifferentOperationsGeneratesSameHashForKeys_1.ast");
    MetadataCacheId operationHash = getIdForMetadataKeys(app, OPERATION_LOCATION);
    MetadataCacheId anotherOperationHash = getIdForMetadataKeys(app, ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(operationHash.toString());
    LOGGER.debug(anotherOperationHash.toString());

    assertThat(operationHash, is(anotherOperationHash));
  }

  @Test
  public void multilevelPartsWithDifferentValuesOnDifferentOperationsGeneratesSameHashForKeys() throws Exception {
    mockMultiLevelMetadataKeyId(operation);
    mockMultiLevelMetadataKeyId(anotherOperation);
    mockTypeResolversInformationModelProperty(operation, CATEGORY_NAME, "outputResolver", "attributesResolver", emptyMap(),
                                              "keysResolver");
    mockTypeResolversInformationModelProperty(anotherOperation, CATEGORY_NAME, "outputResolver", "attributesResolver", emptyMap(),
                                              "keysResolver");

    final var app =
        loadAst("multilevelPartsWithDifferentValuesOnDifferentOperationsGeneratesSameHashForKeys_1.ast");
    MetadataCacheId operationHash = getIdForMetadataKeys(app, OPERATION_LOCATION);
    MetadataCacheId anotherOperationHash = getIdForMetadataKeys(app, ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(operationHash.toString());
    LOGGER.debug(anotherOperationHash.toString());

    assertThat(operationHash, is(anotherOperationHash));
  }

  @Test
  public void partialFetchingWithSameValuesOnDifferentOperationsGeneratesSameHashForKeys() throws Exception {
    mockMultiLevelMetadataKeyId(operation);
    mockMultiLevelMetadataKeyId(anotherOperation);
    setPartialFetchingMock(operation);
    setPartialFetchingMock(anotherOperation);
    mockTypeResolversInformationModelProperty(operation, CATEGORY_NAME, "outputResolver", "attributesResolver", emptyMap(),
                                              "keysResolver");
    mockTypeResolversInformationModelProperty(anotherOperation, CATEGORY_NAME, "outputResolver", "attributesResolver", emptyMap(),
                                              "keysResolver");

    final var app = loadAst("partialFetchingWithSameValuesOnDifferentOperationsGeneratesSameHashForKeys_1.ast");
    MetadataCacheId operationHash = getIdForMetadataKeys(app, OPERATION_LOCATION);
    MetadataCacheId anotherOperationHash = getIdForMetadataKeys(app, ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(operationHash.toString());
    LOGGER.debug(anotherOperationHash.toString());

    assertThat(operationHash, is(anotherOperationHash));
  }

  @Test
  public void partialFetchingWithDifferentValuesOnDifferentOperationsGeneratesSameHashForKeys() throws Exception {
    mockMultiLevelMetadataKeyId(operation);
    mockMultiLevelMetadataKeyId(anotherOperation);
    setPartialFetchingMock(operation);
    setPartialFetchingMock(anotherOperation);

    final var app =
        loadAst("partialFetchingWithDifferentValuesOnDifferentOperationsGeneratesSameHashForKeys_1.ast");
    MetadataCacheId operationHash = getIdForMetadataKeys(app, OPERATION_LOCATION);
    MetadataCacheId anotherOperationHash = getIdForMetadataKeys(app, ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(operationHash.toString());
    LOGGER.debug(anotherOperationHash.toString());

    assertThat(operationHash, is(not((anotherOperationHash))));
  }


  private void mockRequiredForMetadataModelProperty(EnrichableModel model, List<String> parameterNames) {
    if (parameterNames == null) {
      when(model.getModelProperty(RequiredForMetadataModelProperty.class))
          .thenReturn(of(new RequiredForMetadataModelProperty(emptyList())));
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

  private void mockSimpleMetadataKeyId(OperationModel model) {

    ParameterModel metadataKeyId = mockKeyPart(METADATA_KEY_PART_1, 1);

    List<ParameterModel> parameterModels = new ArrayList<>(model.getAllParameterModels());
    parameterModels.add(metadataKeyId);
    when(model.getParameterGroupModels().get(0).getParameterModels()).thenReturn(parameterModels);
    when(model.getParameterGroupModels().get(0).getParameter(anyString()))
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

    when(model.getModelProperty(MetadataResolverFactoryModelProperty.class)).thenReturn(empty());

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
          return empty();
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
    List<ParameterModel> parameterModels = new ArrayList<>(operationModel.getAllParameterModels());
    when(operationModel.getAllParameterModels()).thenReturn(ImmutableList.<ParameterModel>builder()
        .addAll(componentParameterModels)
        .addAll(parameterModels)
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
      return empty();
    });

    when(metadataKeyId.getDslConfiguration()).thenReturn(ParameterDslConfiguration.getDefaultInstance());
    when(metadataKeyId.getLayoutModel()).thenReturn(empty());
    when(metadataKeyId.getRole()).thenReturn(BEHAVIOUR);
    when(metadataKeyId.getType()).thenReturn(TYPE_LOADER.load(String.class));

    return metadataKeyId;
  }

  private ArtifactAst loadAst(String astFileName) throws IOException {
    ArtifactAstDeserializer defaultArtifactAstDeserializer = new ArtifactAstSerializerProvider().getDeserializer();

    ArtifactAst deserializedArtifactAst = defaultArtifactAstDeserializer
        .deserialize(this.getClass().getResourceAsStream("/asts/" + astFileName), name -> extensions.stream()
            .filter(x -> x.getName().equals(name))
            .findFirst()
            .orElse(null));

    return deserializedArtifactAst;
  }

}
