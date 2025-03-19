/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model;

import static org.mule.runtime.config.dsl.model.ComplexActingParameterUtils.forAllComplexActingParameterChanges;

import static java.util.Collections.singletonList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.meta.model.parameter.ActingParameterModel;
import org.mule.runtime.api.meta.model.parameter.FieldValueProviderModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheId;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheIdGenerator;
import org.mule.runtime.metadata.internal.cache.ComponentAstBasedValueProviderCacheIdGenerator;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class ValueProviderCacheIdGeneratorTestCase extends AbstractMockedValueProviderExtensionTestCase {

  private Optional<ValueProviderCacheId> computeIdFor(ArtifactAst app,
                                                      String location,
                                                      String parameterName)
      throws Exception {
    return this.computeIdFor(app, location, parameterName, null);
  }

  private Optional<ValueProviderCacheId> computeIdFor(ArtifactAst app,
                                                      String location,
                                                      String parameterName,
                                                      String targetSelector)
      throws Exception {
    Locator locator = new Locator(app);

    ValueProviderCacheIdGenerator<ComponentAst> componentAstBasedValueProviderCacheIdGenerator =
        new ComponentAstBasedValueProviderCacheIdGenerator(locator);

    ComponentAst component = getComponentAst(app, location);

    Optional<ValueProviderCacheId> astId;
    if (targetSelector == null) {
      astId = componentAstBasedValueProviderCacheIdGenerator.getIdForResolvedValues(component, parameterName);
    } else {
      astId = componentAstBasedValueProviderCacheIdGenerator.getIdForResolvedValues(component, parameterName, targetSelector);
    }

    return astId;
  }

  @Test
  public void idForParameterWithNoProviderInConfig() throws Exception {
    final var app = loadAst("idForParameterWithNoProviderInConfig_1");
    assertThat(computeIdFor(app, MY_CONFIG, ACTING_PARAMETER_NAME)
        .isPresent(), is(false));
  }

  @Test
  public void idForParameterWithNoProviderInSource() throws Exception {
    final var app = loadAst("idForParameterWithNoProviderInConfig_1");
    assertThat(computeIdFor(app, SOURCE_LOCATION, ACTING_PARAMETER_NAME)
        .isPresent(), is(false));
  }

  @Test
  public void idForParameterWithNoProviderInOperation() throws Exception {
    final var app = loadAst("idForParameterWithNoProviderInOperation_1");
    assertThat(computeIdFor(app, OPERATION_LOCATION, ACTING_PARAMETER_NAME)
        .isPresent(), is(false));
  }

  @Test
  public void idForConfigNoChanges() throws Exception {
    final var app = loadAst("idForConfigNoChanges_1");
    Optional<ValueProviderCacheId> configId = computeIdFor(app, MY_CONFIG, PROVIDED_PARAMETER_NAME);
    assertThat(configId.isPresent(), is(true));

    final var modifiedApp = loadAst("idForConfigNoChanges_2");
    checkIdsAreEqual(configId, computeIdFor(modifiedApp, MY_CONFIG, PROVIDED_PARAMETER_NAME));
  }


  @Test
  public void idForConfigChangingNotActingParameters() throws Exception {
    final var app = loadAst("idForConfigChangingNotActingParameters_1");
    Optional<ValueProviderCacheId> configId = computeIdFor(app, MY_CONFIG, PROVIDED_PARAMETER_NAME);
    assertThat(configId.isPresent(), is(true));

    final var modifiedApp = loadAst("idForConfigChangingNotActingParameters_2");
    checkIdsAreEqual(configId, computeIdFor(modifiedApp, MY_CONFIG, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfigChangingActingParameter() throws Exception {
    final var app = loadAst("idForConfigChangingActingParameter_1");
    Optional<ValueProviderCacheId> configId = computeIdFor(app, MY_CONFIG, PROVIDED_PARAMETER_NAME);
    assertThat(configId.isPresent(), is(true));

    final var modifiedApp = loadAst("idForConfigChangingActingParameter_2");
    checkIdsAreDifferent(configId, computeIdFor(modifiedApp, MY_CONFIG, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfigChangingActingParameterInGroup() throws Exception {
    final var app = loadAst("idForConfigChangingActingParameterInGroup_1");
    Optional<ValueProviderCacheId> configId = computeIdFor(app, MY_CONFIG, PROVIDED_PARAMETER_NAME);
    assertThat(configId.isPresent(), is(true));

    final var modifiedApp = loadAst("idForConfigChangingActingParameterInGroup_2");
    checkIdsAreDifferent(configId, computeIdFor(modifiedApp, MY_CONFIG, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfiglessAndConnectionlessOperationNoChanges() throws Exception {
    final var app = loadAst("idForConfiglessAndConnectionlessOperationNoChanges_1");
    Optional<ValueProviderCacheId> opId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));
    final var modifiedApp = loadAst("idForConfiglessAndConnectionlessOperationNoChanges_2");
    checkIdsAreEqual(opId, computeIdFor(modifiedApp, OPERATION_LOCATION,
                                        PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfiglessAndConnectionlessOperationChangingActingParameter() throws Exception {
    final var app = loadAst("idForConfiglessAndConnectionlessOperationChangingActingParameter_1");
    Optional<ValueProviderCacheId> opId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));

    final var modifiedApp = loadAst("idForConfiglessAndConnectionlessOperationChangingActingParameter_2");
    checkIdsAreDifferent(opId, computeIdFor(modifiedApp, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfiglessAndConnectionlessOperationDefaultValueHashIdShouldBeSameWithExplicitValueOnActingParameter()
      throws Exception {
    final var app =
        loadAst("idForConfiglessAndConnectionlessOperationDefaultValueHashIdShouldBeSameWithExplicitValueOnActingParameter_1");
    Optional<ValueProviderCacheId> opId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));

    final var modifiedApp =
        loadAst("idForConfiglessAndConnectionlessOperationDefaultValueHashIdShouldBeSameWithExplicitValueOnActingParameter_2");
    checkIdsAreEqual(opId, computeIdFor(modifiedApp, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfiglessAndConnectionlessOperationChangingActingParameterInGroup() throws Exception {
    final var app = loadAst("idForConfiglessAndConnectionlessOperationChangingActingParameterInGroup_1");
    Optional<ValueProviderCacheId> opId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));

    final var modifiedApp = loadAst("idForConfiglessAndConnectionlessOperationChangingActingParameterInGroup_2");
    checkIdsAreDifferent(opId, computeIdFor(modifiedApp, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfiglessAndConnectionlessOperationChangesInConfig() throws Exception {
    final var app = loadAst("idForConfiglessAndConnectionlessOperationChangesInConfig_1");
    Optional<ValueProviderCacheId> opId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));

    final var modifiedApp = loadAst("idForConfiglessAndConnectionlessOperationChangesInConfig_2");
    checkIdsAreEqual(opId, computeIdFor(modifiedApp, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfiglessAndConnectionlessOperationChangesInConnection() throws Exception {
    final var app = loadAst("idForConfiglessAndConnectionlessOperationChangesInConnection_1");
    Optional<ValueProviderCacheId> opId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));

    final var modifiedApp = loadAst("idForConfiglessAndConnectionlessOperationChangesInConnection_2");
    checkIdsAreEqual(opId, computeIdFor(modifiedApp, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME));
  }


  @Test
  public void idForConfiglessAndConnectionlessSourceNoChanges() throws Exception {
    final var app = loadAst("idForConfiglessAndConnectionlessSourceNoChanges_1");
    Optional<ValueProviderCacheId> sourceId = computeIdFor(app, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(sourceId.isPresent(), is(true));

    final var modifiedAst = loadAst("idForConfiglessAndConnectionlessSourceNoChanges_2");
    checkIdsAreEqual(sourceId, computeIdFor(modifiedAst, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfiglessAndConnectionlessSourceChangingActingParameter() throws Exception {
    final var app = loadAst("idForConfiglessAndConnectionlessSourceChangingActingParameter_1");
    Optional<ValueProviderCacheId> sourceId = computeIdFor(app, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(sourceId.isPresent(), is(true));

    final var modifiedApp = loadAst("idForConfiglessAndConnectionlessSourceChangingActingParameter_2");
    checkIdsAreDifferent(sourceId, computeIdFor(modifiedApp, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfiglessAndConnectionlessSourceChangingActingParameterInGroup() throws Exception {
    final var app = loadAst("idForConfiglessAndConnectionlessSourceChangingActingParameterInGroup_1");
    Optional<ValueProviderCacheId> sourceId = computeIdFor(app, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(sourceId.isPresent(), is(true));

    final var modifiedApp = loadAst("idForConfiglessAndConnectionlessSourceChangingActingParameterInGroup_2");
    checkIdsAreDifferent(sourceId, computeIdFor(modifiedApp, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfiglessAndConnectionlessSourceChangesInConfig() throws Exception {
    final var app = loadAst("idForConfiglessAndConnectionlessSourceChangesInConfig_1");
    Optional<ValueProviderCacheId> sourceId = computeIdFor(app, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(sourceId.isPresent(), is(true));

    final var modifiedApp = loadAst("idForConfiglessAndConnectionlessSourceChangesInConfig_2");
    checkIdsAreEqual(sourceId, computeIdFor(modifiedApp, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfiglessAndConnectionlessSourceChangesInConnection() throws Exception {
    final var app = loadAst("idForConfiglessAndConnectionlessSourceChangesInConnection_1");
    Optional<ValueProviderCacheId> sourceId = computeIdFor(app, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(sourceId.isPresent(), is(true));

    final var modifiedApp = loadAst("idForConfiglessAndConnectionlessSourceChangesInConnection_2");
    checkIdsAreEqual(sourceId, computeIdFor(modifiedApp, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfigAwareOperationChangesInConfigNotRequiredForMetadata() throws Exception {
    when(valueProviderModel.requiresConfiguration()).thenReturn(true);
    final var app = loadAst("idForConfigAwareOperationChangesInConfigNotRequiredForMetadata_1");
    Optional<ValueProviderCacheId> opId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));

    final var modifiedApp = loadAst("idForConfigAwareOperationChangesInConfigNotRequiredForMetadata_2");
    checkIdsAreEqual(opId, computeIdFor(modifiedApp, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfigAwareOperationChangesInConfigRequiredForMetadata() throws Exception {
    when(valueProviderModel.requiresConfiguration()).thenReturn(true);
    final var app = loadAst("idForConfigAwareOperationChangesInConfigRequiredForMetadata_1");
    Optional<ValueProviderCacheId> opId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));

    final var modifiedApp = loadAst("idForConfigAwareOperationChangesInConfigRequiredForMetadata_2");
    checkIdsAreDifferent(opId, computeIdFor(modifiedApp, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConnectionAwareOperationChangesInConnectionNotRequiredForMetadata() throws Exception {
    when(valueProviderModel.requiresConnection()).thenReturn(true);
    final var app = loadAst("idForConnectionAwareOperationChangesInConnectionNotRequiredForMetadata_1");
    Optional<ValueProviderCacheId> opId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));

    final var modifiedApp = loadAst("idForConnectionAwareOperationChangesInConnectionNotRequiredForMetadata_2");
    checkIdsAreEqual(opId, computeIdFor(modifiedApp, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConnectionAwareOperationChangesInConnectionRequiredForMetadata() throws Exception {
    when(valueProviderModel.requiresConnection()).thenReturn(true);
    final var app = loadAst("idForConnectionAwareOperationChangesInConnectionRequiredForMetadata_1");
    Optional<ValueProviderCacheId> opId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));

    final var modifiedApp = loadAst("idForConnectionAwareOperationChangesInConnectionRequiredForMetadata_2");
    checkIdsAreDifferent(opId, computeIdFor(modifiedApp, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfigAwareSourceChangesInConfigNotRequiredForMetadata() throws Exception {
    when(valueProviderModel.requiresConfiguration()).thenReturn(true);
    final var app = loadAst("idForConfigAwareSourceChangesInConfigNotRequiredForMetadata_1");
    Optional<ValueProviderCacheId> sourceId = computeIdFor(app, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(sourceId.isPresent(), is(true));

    final var modifiedApp = loadAst("idForConfigAwareSourceChangesInConfigNotRequiredForMetadata_2");
    checkIdsAreEqual(sourceId, computeIdFor(modifiedApp, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfigAwareSourceChangesInConfigRequiredForMetadata() throws Exception {
    when(valueProviderModel.requiresConfiguration()).thenReturn(true);
    final var app = loadAst("idForConfigAwareSourceChangesInConfigRequiredForMetadata_1");
    Optional<ValueProviderCacheId> sourceId = computeIdFor(app, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(sourceId.isPresent(), is(true));

    final var modifiedApp = loadAst("idForConfigAwareSourceChangesInConfigRequiredForMetadata_2");
    checkIdsAreDifferent(sourceId, computeIdFor(modifiedApp, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConnectionAwareSourceChangesInConnectionNotRequiredForMetadata() throws Exception {
    when(valueProviderModel.requiresConnection()).thenReturn(true);
    final var app = loadAst("idForConnectionAwareSourceChangesInConnectionNotRequiredForMetadata_1");
    Optional<ValueProviderCacheId> sourceId = computeIdFor(app, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(sourceId.isPresent(), is(true));

    final var modifiedApp = loadAst("idForConnectionAwareSourceChangesInConnectionNotRequiredForMetadata_2");
    checkIdsAreEqual(sourceId, computeIdFor(modifiedApp, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConnectionAwareSourceChangesInConnectionRequiredForMetadata() throws Exception {
    when(valueProviderModel.requiresConnection()).thenReturn(true);
    final var app = loadAst("idForConnectionAwareSourceChangesInConnectionRequiredForMetadata_1");
    Optional<ValueProviderCacheId> sourceId = computeIdFor(app, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(sourceId.isPresent(), is(true));

    final var modifiedApp = loadAst("idForConnectionAwareSourceChangesInConnectionRequiredForMetadata_2");
    checkIdsAreDifferent(sourceId, computeIdFor(modifiedApp, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void equalConfigsWithDifferentNameGetSameHash() throws Exception {
    final var app = loadAst("equalConfigsWithDifferentNameGetSameHash_1");
    Optional<ValueProviderCacheId> config1Id = computeIdFor(app, MY_CONFIG, PROVIDED_PARAMETER_NAME);
    Optional<ValueProviderCacheId> config2Id = computeIdFor(app, "newName", PROVIDED_PARAMETER_NAME);
    checkIdsAreEqual(config1Id, config2Id);
  }

  @Test
  public void differentConfigsWithSameParameterGetSameHash() throws Exception {
    final var app = loadAst("differentConfigsWithSameParameterGetSameHash_1");
    Optional<ValueProviderCacheId> config1Id = computeIdFor(app, MY_CONFIG, PROVIDED_PARAMETER_NAME);
    Optional<ValueProviderCacheId> config2Id = computeIdFor(app, "newName", PROVIDED_PARAMETER_NAME);
    checkIdsAreEqual(config1Id, config2Id);
  }

  @Test
  public void differentValueProviderNameGetsSameHash() throws Exception {
    when(valueProviderModel.requiresConnection()).thenReturn(true);
    when(valueProviderModel.requiresConfiguration()).thenReturn(true);
    final var app = loadAst("differentValueProviderNameGetsSameHash_1");
    Optional<ValueProviderCacheId> opId1 = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);

    when(valueProviderModel.getProviderName()).thenReturn("newValueProviderName");
    final var modifiedApp = loadAst("differentValueProviderNameGetsSameHash_2");
    Optional<ValueProviderCacheId> opId2 = computeIdFor(modifiedApp, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    checkIdsAreEqual(opId1, opId2);
  }

  @Test
  public void differentValueProviderIdGetsDifferentHash() throws Exception {
    when(valueProviderModel.requiresConnection()).thenReturn(true);
    when(valueProviderModel.requiresConfiguration()).thenReturn(true);
    final var app = loadAst("differentValueProviderIdGetsDifferentHash_1");
    Optional<ValueProviderCacheId> opId1 = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);

    when(valueProviderModel.getProviderId()).thenReturn("newValueProviderId");
    final var modifiedApp = loadAst("differentValueProviderIdGetsDifferentHash_2");
    Optional<ValueProviderCacheId> opId2 = computeIdFor(modifiedApp, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    checkIdsAreDifferent(opId1, opId2);
  }

  @Test
  public void differentOperationsWithSameParametersGetsSameHash() throws Exception {
    final var app = loadAst("differentOperationsWithSameParametersGetsSameHash_1");
    Optional<ValueProviderCacheId> opId1 = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    Optional<ValueProviderCacheId> opId2 = computeIdFor(app, OTHER_OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    checkIdsAreEqual(opId1, opId2);
  }

  @Test
  public void differentHashForComplexActingParameterValue() throws Exception {
    List<Optional<ValueProviderCacheId>> allIds = new LinkedList<>();
    AtomicInteger index = new AtomicInteger();
    forAllComplexActingParameterChanges(OPERATION_LOCATION,
                                        COMPLEX_ACTING_PARAMETER_NAME,
                                        v -> allIds
                                            .add(computeIdFor(loadAst("differentHashForComplexActingParameterValue_"
                                                + index.incrementAndGet()),
                                                              OPERATION_LOCATION,
                                                              PROVIDED_FROM_COMPLEX_PARAMETER_NAME)));

    for (Optional<ValueProviderCacheId> idA : allIds) {
      for (Optional<ValueProviderCacheId> idB : allIds) {
        if (idA != idB) {
          checkIdsAreDifferent(idA, idB);
        }
      }
    }
  }

  @Test
  public void extractionExpressionIsUsedForActingParameters() throws Exception {
    final String extractionExpression = "actingParameter";
    final String otherParameterName = "otherParameterName";
    ActingParameterModel actingParameterModel = createActingParameterModel(otherParameterName);
    when(actingParameterModel.getExtractionExpression()).thenReturn(extractionExpression);
    when(valueProviderModel.getActingParameters()).thenReturn(singletonList(otherParameterName));
    when(valueProviderModel.getParameters()).thenReturn(singletonList(actingParameterModel));

    final var app = loadAst("extractionExpressionIsUsedForActingParameters_1");
    Optional<ValueProviderCacheId> operationId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(operationId.isPresent(), is(true));

    final var modifiedApp = loadAst("extractionExpressionIsUsedForActingParameters_2");
    checkIdsAreDifferent(operationId, computeIdFor(modifiedApp, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  // TODO(CMTS-208): This should fail once implemented
  // Ideally, we would want to only use the actual field but that would require us to parse the extraction expression to actually
  // find the field required. For now, the whole parameter will be used.
  public void wholeParameterIsUsedIfExpressionPointsToField() throws Exception {
    final String extractionExpression = COMPLEX_ACTING_PARAMETER_NAME + ".stringParam";
    ActingParameterModel actingParameterModel = createActingParameterModel(ACTING_PARAMETER_NAME);
    when(actingParameterModel.getExtractionExpression()).thenReturn(extractionExpression);
    when(valueProviderModel.getParameters()).thenReturn(singletonList(actingParameterModel));

    final var app = loadAst("wholeParameterIsUsedIfExpressionPointsToField_1");
    Optional<ValueProviderCacheId> operationId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_FROM_COMPLEX_PARAMETER_NAME);
    assertThat(operationId.isPresent(), is(true));

    // Modify a parameter that should not affect the hash
    final var modifiedApp = loadAst("wholeParameterIsUsedIfExpressionPointsToField_2");
    checkIdsAreDifferent(operationId, computeIdFor(modifiedApp, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void presentFieldValueProviderGetsId() throws Exception {
    final String targetSelector = "some.target.path";
    FieldValueProviderModel fieldValueProviderModel = createFieldValueProviderModel(FIELD_VALUE_PROVIDER_NAME,
                                                                                    FIELD_VALUE_PROVIDER_ID,
                                                                                    targetSelector);
    when(providedParameter.getFieldValueProviderModels()).thenReturn(singletonList(fieldValueProviderModel));

    final var app = loadAst("presentFieldValueProviderGetsId_1");
    Optional<ValueProviderCacheId> cacheId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME, targetSelector);
    assertThat(cacheId.isPresent(), equalTo(true));
    cacheId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME, "other.field");
    assertThat(cacheId.isPresent(), is(false));
  }

  @Test
  // TODO(CMTS-208): This should fail once implemented
  // Ideally we would want only the acting fields to modify the hash, but since there is still no way
  // to correctly identify them without evaluating the path expression, we are using the whole parameter for caching
  public void changesInParameterWithActingFieldReturnsDifferentHash() throws Exception {
    final String targetSelector = "some.target.path";
    FieldValueProviderModel fieldValueProviderModel =
        createFieldValueProviderModel(FIELD_VALUE_PROVIDER_NAME, FIELD_VALUE_PROVIDER_ID, targetSelector);

    ActingParameterModel actingParameterModel = createActingParameterModel(COMPLEX_ACTING_PARAMETER_NAME,
                                                                           COMPLEX_ACTING_PARAMETER_NAME
                                                                               + ".innerPojoParam.stringParam");

    when(fieldValueProviderModel.getParameters()).thenReturn(singletonList(actingParameterModel));
    when(providedParameterFromComplex.getFieldValueProviderModels()).thenReturn(singletonList(fieldValueProviderModel));

    List<Optional<ValueProviderCacheId>> allIds = new LinkedList<>();
    AtomicInteger index = new AtomicInteger();
    forAllComplexActingParameterChanges(OPERATION_LOCATION, COMPLEX_ACTING_PARAMETER_NAME,
                                        v -> allIds
                                            .add(computeIdFor(loadAst("changesInParameterWithActingFieldReturnsDifferentHash_"
                                                + index.incrementAndGet()),
                                                              OPERATION_LOCATION,
                                                              PROVIDED_FROM_COMPLEX_PARAMETER_NAME,
                                                              targetSelector)));

    // Every id in the list should be different to each other
    for (Optional<ValueProviderCacheId> idA : allIds) {
      for (Optional<ValueProviderCacheId> idB : allIds) {
        if (idA != idB) {
          checkIdsAreDifferent(idA, idB);
        }
      }
    }
  }

  @Test
  public void actingFieldFromNotExistentParameterIsNotConsideredForId() throws Exception {
    final String targetSelector = "some.target.path";
    FieldValueProviderModel fieldValueProviderModel =
        createFieldValueProviderModel(FIELD_VALUE_PROVIDER_NAME, FIELD_VALUE_PROVIDER_ID, targetSelector);

    ActingParameterModel actingParameterModel = createActingParameterModel(ACTING_PARAMETER_NAME,
                                                                           "notExistentParam.stringParam");

    when(fieldValueProviderModel.getParameters()).thenReturn(singletonList(actingParameterModel));
    when(providedParameter.getFieldValueProviderModels()).thenReturn(singletonList(fieldValueProviderModel));

    final var app = loadAst("actingFieldFromNotExistentParameterIsNotConsideredForId_1");
    Optional<ValueProviderCacheId> id = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME, targetSelector);
    assertThat(id.isPresent(), is(true));
  }

  @Test
  public void actingFieldAsExpressionUsesWholeParameter() throws Exception {
    final String targetSelector = "some.target.path";
    FieldValueProviderModel fieldValueProviderModel =
        createFieldValueProviderModel(FIELD_VALUE_PROVIDER_NAME, FIELD_VALUE_PROVIDER_ID, targetSelector);

    ActingParameterModel actingParameterModel = createActingParameterModel(COMPLEX_ACTING_PARAMETER_NAME,
                                                                           COMPLEX_ACTING_PARAMETER_NAME
                                                                               + ".innerPojoParam.stringParam");

    when(fieldValueProviderModel.getParameters()).thenReturn(singletonList(actingParameterModel));
    when(providedParameterFromComplex.getFieldValueProviderModels()).thenReturn(singletonList(fieldValueProviderModel));

    final var app = loadAst("actingFieldAsExpressionUsesWholeParameter_1");
    Optional<ValueProviderCacheId> originalExpressionId =
        computeIdFor(app, OPERATION_LOCATION, PROVIDED_FROM_COMPLEX_PARAMETER_NAME, targetSelector);

    final var modifiedApp = loadAst("actingFieldAsExpressionUsesWholeParameter_2");
    Optional<ValueProviderCacheId> otherExpressionId =
        computeIdFor(modifiedApp, OPERATION_LOCATION, PROVIDED_FROM_COMPLEX_PARAMETER_NAME, targetSelector);

    checkIdsAreDifferent(originalExpressionId, otherExpressionId);
  }

  @Test
  public void invalidTargetSelector() throws Exception {
    final var app = loadAst("invalidTargetSelector_1");
    assertThat(computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME, "this-is-not&a$$$val*d@path").isPresent(),
               is(false));
  }
}
