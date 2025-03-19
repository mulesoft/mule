/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheId;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheIdGenerator;
import org.mule.runtime.metadata.api.locator.ComponentLocator;
import org.mule.runtime.metadata.internal.cache.ComponentAstBasedValueProviderCacheIdGenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

public class ComponentAstValueProviderCacheIdGeneratorTestCase extends AbstractMockedValueProviderExtensionTestCase {

  private Optional<ValueProviderCacheId> computeIdFor(ArtifactAst app,
                                                      String location,
                                                      String parameterName)
      throws Exception {
    Locator locator = new Locator(app);
    ValueProviderCacheIdGenerator cacheIdGenerator = new ComponentAstBasedValueProviderCacheIdGenerator(locator);
    ComponentAst component = getComponentAst(app, location);
    return cacheIdGenerator.getIdForResolvedValues(component, parameterName);
  }

  @Test
  public void idForParameterWithNoProviderInConfig() throws Exception {
    final var app = loadAst("idForParameterWithNoProviderInConfig_1");
    assertThat(computeIdFor(app, MY_CONFIG, ACTING_PARAMETER_NAME).isPresent(),
               is(false));
  }

  @Test
  public void idForParameterWithNoProviderInSource() throws Exception {
    final var app = loadAst("idForParameterWithNoProviderInSource_1");
    assertThat(computeIdFor(app, SOURCE_LOCATION, ACTING_PARAMETER_NAME).isPresent(),
               is(false));
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
    checkIdsAreEqual(configId, computeIdFor(loadAst(this.name.getMethodName() + "_2"), MY_CONFIG, PROVIDED_PARAMETER_NAME));
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
    checkIdsAreEqual(opId, computeIdFor(modifiedApp, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME));
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

    final var modifiedApp = loadAst("idForConfiglessAndConnectionlessSourceNoChanges_2");
    checkIdsAreEqual(sourceId, computeIdFor(modifiedApp, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME));
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
  public void differentConfigsWithDifferentProviderIdGetDifferentHash() throws Exception {
    final var app = loadAst("differentConfigsWithDifferentProviderIdGetDifferentHash_1");
    Optional<ValueProviderCacheId> config1Id = computeIdFor(app, MY_CONFIG, PROVIDED_PARAMETER_NAME);

    when(valueProviderModel.getProviderId()).thenReturn("newValueProviderId");
    final var modifiedApp = loadAst("differentConfigsWithDifferentProviderIdGetDifferentHash_2");
    Optional<ValueProviderCacheId> config2Id = computeIdFor(modifiedApp, "newName", PROVIDED_PARAMETER_NAME);
    checkIdsAreDifferent(config1Id, config2Id);
  }

  @Test
  public void differentConfigsWithSameProviderIdGetSameHash() throws Exception {
    final var app = loadAst("differentConfigsWithSameProviderIdGetSameHash_1");
    Optional<ValueProviderCacheId> config1Id = computeIdFor(app, MY_CONFIG, PROVIDED_PARAMETER_NAME);
    Optional<ValueProviderCacheId> config2Id = computeIdFor(app, "newName", PROVIDED_PARAMETER_NAME);
    checkIdsAreEqual(config1Id, config2Id);
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
  public void differentOperationsWithDifferentProviderIdGetsDifferentHash() throws Exception {
    final var app = loadAst("differentOperationsWithDifferentProviderIdGetsDifferentHash_1");
    Optional<ValueProviderCacheId> opId1 = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);

    when(valueProviderModel.getProviderId()).thenReturn("newValueProviderId");
    final var modifiedApp = loadAst("differentOperationsWithDifferentProviderIdGetsDifferentHash_2");
    Optional<ValueProviderCacheId> opId2 = computeIdFor(modifiedApp, OTHER_OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    checkIdsAreDifferent(opId1, opId2);
  }

  @Test
  public void differentOperationsWithSameValueProviderIdGetsSameHash() throws Exception {
    final var app = loadAst("differentOperationsWithSameValueProviderIdGetsSameHash_1");
    Optional<ValueProviderCacheId> opId1 = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    Optional<ValueProviderCacheId> opId2 = computeIdFor(app, OTHER_OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    checkIdsAreEqual(opId1, opId2);
  }

  private static class Locator implements ComponentLocator<ComponentAst> {

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
