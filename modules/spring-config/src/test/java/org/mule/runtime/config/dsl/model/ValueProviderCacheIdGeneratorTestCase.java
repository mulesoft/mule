/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model;

import static org.mule.runtime.app.declaration.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.runtime.app.declaration.api.fluent.ParameterSimpleValue.plain;
import static org.mule.runtime.config.dsl.model.ComplexActingParameterUtils.forAllComplexActingParameterChanges;
import static org.mule.runtime.config.dsl.model.DeclarationUtils.modifyParameter;
import static org.mule.runtime.config.dsl.model.DeclarationUtils.removeParameter;

import static java.lang.String.format;
import static java.util.Collections.singletonList;

import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.meta.model.parameter.ActingParameterModel;
import org.mule.runtime.api.meta.model.parameter.FieldValueProviderModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.ConfigurationElementDeclaration;
import org.mule.runtime.app.declaration.api.ElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterValue;
import org.mule.runtime.app.declaration.api.ParameterizedElementDeclaration;
import org.mule.runtime.app.declaration.api.fluent.ParameterObjectValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterSimpleValue;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.config.api.dsl.model.metadata.ComponentBasedValueProviderCacheIdGenerator;
import org.mule.runtime.config.api.dsl.model.metadata.DeclarationBasedValueProviderCacheIdGenerator;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheId;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheIdGenerator;
import org.mule.runtime.metadata.api.dsl.DslElementModel;
import org.mule.runtime.metadata.api.locator.ComponentLocator;
import org.mule.runtime.metadata.internal.cache.ComponentAstBasedValueProviderCacheIdGenerator;
import org.mule.runtime.metadata.internal.cache.DslElementBasedValueProviderCacheIdGenerator;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class ValueProviderCacheIdGeneratorTestCase extends AbstractMockedValueProviderExtensionTestCase {

  private DslElementModelFactory dslElementModelFactory;

  @Override
  public void before() {
    super.before();
    dslElementModelFactory = DslElementModelFactory.getDefault(dslContext);
  }

  private Optional<ValueProviderCacheId> computeIdFor(ArtifactAst app,
                                                      ArtifactDeclaration appDeclaration,
                                                      String location,
                                                      String parameterName)
      throws Exception {
    return this.computeIdFor(app, appDeclaration, location, parameterName, null);
  }

  private Optional<ValueProviderCacheId> computeIdFor(ArtifactAst app,
                                                      ArtifactDeclaration appDeclaration,
                                                      String location,
                                                      String parameterName,
                                                      String targetSelector)
      throws Exception {
    Locator locator = new Locator(app);
    ComponentLocator<DslElementModel<?>> dslLocator =
        l -> getDeclaration(appDeclaration, l.toString()).map(d -> dslElementModelFactory.create(d).orElse(null));

    ComponentLocator<ElementDeclaration> declarationLocator =
        l -> appDeclaration.findElement(builderFromStringRepresentation(l.toString()).build());

    ValueProviderCacheIdGenerator<ComponentAst> componentAstBasedValueProviderCacheIdGenerator =
        new ComponentAstBasedValueProviderCacheIdGenerator(locator);
    ValueProviderCacheIdGenerator<ComponentAst> componentBasedValueProviderCacheIdGenerator =
        new ComponentBasedValueProviderCacheIdGenerator(dslContext, locator);
    ValueProviderCacheIdGenerator<DslElementModel<?>> dslElementModelValueProviderCacheIdGenerator =
        new DslElementBasedValueProviderCacheIdGenerator(dslLocator);
    ValueProviderCacheIdGenerator<ElementDeclaration> elementDeclarationValueProviderCacheIdGenerator =
        new DeclarationBasedValueProviderCacheIdGenerator(dslContext, declarationLocator);

    ComponentAst component = getComponentAst(app, location);
    DslElementModel<?> dslElementModel = dslLocator.get(Location.builderFromStringRepresentation(location).build())
        .orElseThrow(() -> new AssertionError("Could not create dslElementModel"));

    Optional<ParameterizedElementDeclaration> elementDeclaration =
        appDeclaration.findElement(builderFromStringRepresentation(location).build());
    Optional<ParameterizedModel> elementModel = component.getModel(ParameterizedModel.class);

    if (!elementDeclaration.isPresent() || !elementModel.isPresent()) {
      fail(format("missing declaration or model for: %s", location));
    }

    Optional<ValueProviderCacheId> dslElementId;
    Optional<ValueProviderCacheId> componentBasedId;
    Optional<ValueProviderCacheId> declarationBasedId;
    Optional<ValueProviderCacheId> astId;
    if (targetSelector == null) {
      dslElementId = dslElementModelValueProviderCacheIdGenerator.getIdForResolvedValues(dslElementModel, parameterName);
      componentBasedId = componentBasedValueProviderCacheIdGenerator.getIdForResolvedValues(component, parameterName);
      declarationBasedId =
          elementDeclarationValueProviderCacheIdGenerator.getIdForResolvedValues(elementDeclaration.get(), parameterName);
      astId = componentAstBasedValueProviderCacheIdGenerator.getIdForResolvedValues(component, parameterName);
    } else {
      dslElementId =
          dslElementModelValueProviderCacheIdGenerator.getIdForResolvedValues(dslElementModel, parameterName, targetSelector);
      componentBasedId =
          componentBasedValueProviderCacheIdGenerator.getIdForResolvedValues(component, parameterName, targetSelector);
      declarationBasedId = elementDeclarationValueProviderCacheIdGenerator.getIdForResolvedValues(elementDeclaration.get(),
                                                                                                  parameterName, targetSelector);
      astId = componentAstBasedValueProviderCacheIdGenerator.getIdForResolvedValues(component, parameterName, targetSelector);
    }

    checkIdsAreEqual(astId, dslElementId);
    checkIdsAreEqual(dslElementId, componentBasedId);
    checkIdsAreEqual(componentBasedId, declarationBasedId);

    // Any should be fine
    return dslElementId;
  }

  @Test
  public void idForParameterWithNoProviderInConfig() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    assertThat(computeIdFor(loadAst("idForParameterWithNoProviderInConfig_1"), app, MY_CONFIG, ACTING_PARAMETER_NAME)
        .isPresent(), is(false));
  }

  @Test
  public void idForParameterWithNoProviderInSource() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    assertThat(computeIdFor(loadAst("idForParameterWithNoProviderInConfig_1"), app, SOURCE_LOCATION, ACTING_PARAMETER_NAME)
        .isPresent(), is(false));
  }

  @Test
  public void idForParameterWithNoProviderInOperation() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    assertThat(computeIdFor(loadAst("idForParameterWithNoProviderInOperation_1"), app, OPERATION_LOCATION, ACTING_PARAMETER_NAME)
        .isPresent(), is(false));
  }

  @Test
  public void idForConfigNoChanges() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> configId =
        computeIdFor(loadAst("idForConfigNoChanges_1"), app, MY_CONFIG, PROVIDED_PARAMETER_NAME);
    assertThat(configId.isPresent(), is(true));
    checkIdsAreEqual(configId,
                     computeIdFor(loadAst("idForConfigNoChanges_2"), app, MY_CONFIG, PROVIDED_PARAMETER_NAME));
  }


  @Test
  public void idForConfigChangingNotActingParameters() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> configId =
        computeIdFor(loadAst("idForConfigChangingNotActingParameters_1"), app, MY_CONFIG, PROVIDED_PARAMETER_NAME);
    assertThat(configId.isPresent(), is(true));
    modifyParameter(app, MY_CONFIG, PARAMETER_REQUIRED_FOR_METADATA_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreEqual(configId,
                     computeIdFor(loadAst("idForConfigChangingNotActingParameters_2"), app, MY_CONFIG, PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfigChangingActingParameter() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> configId =
        computeIdFor(loadAst("idForConfigChangingActingParameter_1"), app, MY_CONFIG, PROVIDED_PARAMETER_NAME);
    assertThat(configId.isPresent(), is(true));
    modifyParameter(app, MY_CONFIG, ACTING_PARAMETER_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreDifferent(configId, computeIdFor(loadAst("idForConfigChangingActingParameter_2"), app, MY_CONFIG,
                                                PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfigChangingActingParameterInGroup() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> configId =
        computeIdFor(loadAst("idForConfigChangingActingParameterInGroup_1"), app, MY_CONFIG, PROVIDED_PARAMETER_NAME);
    assertThat(configId.isPresent(), is(true));
    modifyParameter(app, MY_CONFIG, PARAMETER_IN_GROUP_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreDifferent(configId, computeIdFor(loadAst("idForConfigChangingActingParameterInGroup_2"), app, MY_CONFIG,
                                                PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfiglessAndConnectionlessOperationNoChanges() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> opId =
        computeIdFor(loadAst("idForConfiglessAndConnectionlessOperationNoChanges_1"), app, OPERATION_LOCATION,
                     PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));
    checkIdsAreEqual(opId, computeIdFor(loadAst("idForConfiglessAndConnectionlessOperationNoChanges_2"), app, OPERATION_LOCATION,
                                        PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfiglessAndConnectionlessOperationChangingActingParameter() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> opId =
        computeIdFor(loadAst("idForConfiglessAndConnectionlessOperationChangingActingParameter_1"), app, OPERATION_LOCATION,
                     PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));
    modifyParameter(app, OPERATION_LOCATION, ACTING_PARAMETER_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreDifferent(opId,
                         computeIdFor(loadAst("idForConfiglessAndConnectionlessOperationChangingActingParameter_2"), app,
                                      OPERATION_LOCATION,
                                      PROVIDED_PARAMETER_NAME));
  }


  @Test
  public void idForConfiglessAndConnectionlessOperationDefaultValueHashIdShouldBeSameWithExplicitValueOnActingParameter()
      throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> opId =
        computeIdFor(loadAst("idForConfiglessAndConnectionlessOperationDefaultValueHashIdShouldBeSameWithExplicitValueOnActingParameter_1"),
                     app, OPERATION_LOCATION,
                     PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));
    removeParameter(app, OPERATION_LOCATION, ACTING_PARAMETER_NAME);
    checkIdsAreEqual(opId,
                     computeIdFor(loadAst("idForConfiglessAndConnectionlessOperationDefaultValueHashIdShouldBeSameWithExplicitValueOnActingParameter_2"),
                                  app,
                                  OPERATION_LOCATION,
                                  PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfiglessAndConnectionlessOperationChangingActingParameterInGroup() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> opId =
        computeIdFor(loadAst("idForConfiglessAndConnectionlessOperationChangingActingParameterInGroup_1"), app,
                     OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));
    modifyParameter(app, OPERATION_LOCATION, PARAMETER_IN_GROUP_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreDifferent(opId,
                         computeIdFor(loadAst("idForConfiglessAndConnectionlessOperationChangingActingParameterInGroup_2"), app,
                                      OPERATION_LOCATION,
                                      PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfiglessAndConnectionlessOperationChangesInConfig() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> opId =
        computeIdFor(loadAst("idForConfiglessAndConnectionlessOperationChangesInConfig_1"), app, OPERATION_LOCATION,
                     PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));
    modifyParameter(app, MY_CONFIG, PARAMETER_REQUIRED_FOR_METADATA_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    modifyParameter(app, MY_CONFIG, ACTING_PARAMETER_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreEqual(opId,
                     computeIdFor(loadAst("idForConfiglessAndConnectionlessOperationChangesInConfig_2"), app, OPERATION_LOCATION,
                                  PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfiglessAndConnectionlessOperationChangesInConnection() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> opId =
        computeIdFor(loadAst("idForConfiglessAndConnectionlessOperationChangesInConnection_1"), app, OPERATION_LOCATION,
                     PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));
    modifyParameter(app, MY_CONNECTION, PARAMETER_REQUIRED_FOR_METADATA_NAME,
                    p -> p.setValue(ParameterSimpleValue.of("newValue")));
    modifyParameter(app, MY_CONNECTION, ACTING_PARAMETER_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreEqual(opId,
                     computeIdFor(loadAst("idForConfiglessAndConnectionlessOperationChangesInConnection_2"), app,
                                  OPERATION_LOCATION,
                                  PROVIDED_PARAMETER_NAME));
  }


  @Test
  public void idForConfiglessAndConnectionlessSourceNoChanges() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> sourceId =
        computeIdFor(loadAst("idForConfiglessAndConnectionlessSourceNoChanges_1"), app, SOURCE_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(sourceId.isPresent(), is(true));
    checkIdsAreEqual(sourceId, computeIdFor(loadAst("idForConfiglessAndConnectionlessSourceNoChanges_2"), app, SOURCE_LOCATION,
                                            PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfiglessAndConnectionlessSourceChangingActingParameter() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> sourceId =
        computeIdFor(loadAst("idForConfiglessAndConnectionlessSourceChangingActingParameter_1"), app, SOURCE_LOCATION,
                     PROVIDED_PARAMETER_NAME);
    assertThat(sourceId.isPresent(), is(true));
    modifyParameter(app, SOURCE_LOCATION, ACTING_PARAMETER_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreDifferent(sourceId,
                         computeIdFor(loadAst("idForConfiglessAndConnectionlessSourceChangingActingParameter_2"), app,
                                      SOURCE_LOCATION,
                                      PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfiglessAndConnectionlessSourceChangingActingParameterInGroup() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> sourceId =
        computeIdFor(loadAst("idForConfiglessAndConnectionlessSourceChangingActingParameterInGroup_1"), app, SOURCE_LOCATION,
                     PROVIDED_PARAMETER_NAME);
    assertThat(sourceId.isPresent(), is(true));
    modifyParameter(app, SOURCE_LOCATION, PARAMETER_IN_GROUP_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreDifferent(sourceId,
                         computeIdFor(loadAst("idForConfiglessAndConnectionlessSourceChangingActingParameterInGroup_2"), app,
                                      SOURCE_LOCATION,
                                      PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfiglessAndConnectionlessSourceChangesInConfig() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> sourceId =
        computeIdFor(loadAst("idForConfiglessAndConnectionlessSourceChangesInConfig_1"), app, SOURCE_LOCATION,
                     PROVIDED_PARAMETER_NAME);
    assertThat(sourceId.isPresent(), is(true));
    modifyParameter(app, MY_CONFIG, PARAMETER_REQUIRED_FOR_METADATA_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    modifyParameter(app, MY_CONFIG, ACTING_PARAMETER_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreEqual(sourceId,
                     computeIdFor(loadAst("idForConfiglessAndConnectionlessSourceChangesInConfig_2"), app, SOURCE_LOCATION,
                                  PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfiglessAndConnectionlessSourceChangesInConnection() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> sourceId =
        computeIdFor(loadAst("idForConfiglessAndConnectionlessSourceChangesInConnection_1"), app, SOURCE_LOCATION,
                     PROVIDED_PARAMETER_NAME);
    assertThat(sourceId.isPresent(), is(true));
    modifyParameter(app, MY_CONNECTION, PARAMETER_REQUIRED_FOR_METADATA_NAME,
                    p -> p.setValue(ParameterSimpleValue.of("newValue")));
    modifyParameter(app, MY_CONNECTION, ACTING_PARAMETER_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreEqual(sourceId,
                     computeIdFor(loadAst("idForConfiglessAndConnectionlessSourceChangesInConnection_2"), app, SOURCE_LOCATION,
                                  PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfigAwareOperationChangesInConfigNotRequiredForMetadata() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    when(valueProviderModel.requiresConfiguration()).thenReturn(true);
    Optional<ValueProviderCacheId> opId =
        computeIdFor(loadAst("idForConfigAwareOperationChangesInConfigNotRequiredForMetadata_1"), app, OPERATION_LOCATION,
                     PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));
    modifyParameter(app, MY_CONFIG, ACTING_PARAMETER_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreEqual(opId,
                     computeIdFor(loadAst("idForConfigAwareOperationChangesInConfigNotRequiredForMetadata_2"), app,
                                  OPERATION_LOCATION,
                                  PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfigAwareOperationChangesInConfigRequiredForMetadata() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    when(valueProviderModel.requiresConfiguration()).thenReturn(true);
    Optional<ValueProviderCacheId> opId =
        computeIdFor(loadAst("idForConfigAwareOperationChangesInConfigRequiredForMetadata_1"), app, OPERATION_LOCATION,
                     PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));
    modifyParameter(app, MY_CONFIG, PARAMETER_REQUIRED_FOR_METADATA_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreDifferent(opId,
                         computeIdFor(loadAst("idForConfigAwareOperationChangesInConfigRequiredForMetadata_2"), app,
                                      OPERATION_LOCATION,
                                      PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConnectionAwareOperationChangesInConnectionNotRequiredForMetadata() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    when(valueProviderModel.requiresConnection()).thenReturn(true);
    Optional<ValueProviderCacheId> opId =
        computeIdFor(loadAst("idForConnectionAwareOperationChangesInConnectionNotRequiredForMetadata_1"), app, OPERATION_LOCATION,
                     PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));
    modifyParameter(app, MY_CONNECTION, ACTING_PARAMETER_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreEqual(opId,
                     computeIdFor(loadAst("idForConnectionAwareOperationChangesInConnectionNotRequiredForMetadata_2"), app,
                                  OPERATION_LOCATION,
                                  PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConnectionAwareOperationChangesInConnectionRequiredForMetadata() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    when(valueProviderModel.requiresConnection()).thenReturn(true);
    Optional<ValueProviderCacheId> opId =
        computeIdFor(loadAst("idForConnectionAwareOperationChangesInConnectionRequiredForMetadata_1"), app, OPERATION_LOCATION,
                     PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));
    modifyParameter(app, MY_CONNECTION, PARAMETER_REQUIRED_FOR_METADATA_NAME,
                    p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreDifferent(opId,
                         computeIdFor(loadAst("idForConnectionAwareOperationChangesInConnectionRequiredForMetadata_2"), app,
                                      OPERATION_LOCATION,
                                      PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfigAwareSourceChangesInConfigNotRequiredForMetadata() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    when(valueProviderModel.requiresConfiguration()).thenReturn(true);
    Optional<ValueProviderCacheId> sourceId =
        computeIdFor(loadAst("idForConfigAwareSourceChangesInConfigNotRequiredForMetadata_1"), app, SOURCE_LOCATION,
                     PROVIDED_PARAMETER_NAME);
    assertThat(sourceId.isPresent(), is(true));
    modifyParameter(app, MY_CONFIG, ACTING_PARAMETER_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreEqual(sourceId,
                     computeIdFor(loadAst("idForConfigAwareSourceChangesInConfigNotRequiredForMetadata_2"), app, SOURCE_LOCATION,
                                  PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConfigAwareSourceChangesInConfigRequiredForMetadata() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    when(valueProviderModel.requiresConfiguration()).thenReturn(true);
    Optional<ValueProviderCacheId> sourceId =
        computeIdFor(loadAst("idForConfigAwareSourceChangesInConfigRequiredForMetadata_1"), app, SOURCE_LOCATION,
                     PROVIDED_PARAMETER_NAME);
    assertThat(sourceId.isPresent(), is(true));
    modifyParameter(app, MY_CONFIG, PARAMETER_REQUIRED_FOR_METADATA_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreDifferent(sourceId,
                         computeIdFor(loadAst("idForConfigAwareSourceChangesInConfigRequiredForMetadata_2"), app, SOURCE_LOCATION,
                                      PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConnectionAwareSourceChangesInConnectionNotRequiredForMetadata() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    when(valueProviderModel.requiresConnection()).thenReturn(true);
    Optional<ValueProviderCacheId> sourceId =
        computeIdFor(loadAst("idForConnectionAwareSourceChangesInConnectionNotRequiredForMetadata_1"), app, SOURCE_LOCATION,
                     PROVIDED_PARAMETER_NAME);
    assertThat(sourceId.isPresent(), is(true));
    modifyParameter(app, MY_CONNECTION, ACTING_PARAMETER_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreEqual(sourceId,
                     computeIdFor(loadAst("idForConnectionAwareSourceChangesInConnectionNotRequiredForMetadata_2"), app,
                                  SOURCE_LOCATION,
                                  PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void idForConnectionAwareSourceChangesInConnectionRequiredForMetadata() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    when(valueProviderModel.requiresConnection()).thenReturn(true);
    Optional<ValueProviderCacheId> sourceId =
        computeIdFor(loadAst("idForConnectionAwareSourceChangesInConnectionRequiredForMetadata_1"), app, SOURCE_LOCATION,
                     PROVIDED_PARAMETER_NAME);
    assertThat(sourceId.isPresent(), is(true));
    modifyParameter(app, MY_CONNECTION, PARAMETER_REQUIRED_FOR_METADATA_NAME,
                    p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreDifferent(sourceId,
                         computeIdFor(loadAst("idForConnectionAwareSourceChangesInConnectionRequiredForMetadata_2"), app,
                                      SOURCE_LOCATION,
                                      PROVIDED_PARAMETER_NAME));
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
    Optional<ValueProviderCacheId> config1Id =
        computeIdFor(loadAst("equalConfigsWithDifferentNameGetSameHash_1"), app, MY_CONFIG, PROVIDED_PARAMETER_NAME);
    Optional<ValueProviderCacheId> config2Id =
        computeIdFor(loadAst("equalConfigsWithDifferentNameGetSameHash_1"), app, "newName", PROVIDED_PARAMETER_NAME);
    checkIdsAreEqual(config1Id, config2Id);
  }

  @Test
  public void differentConfigsWithSameParameterGetSameHash() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    ConfigurationElementDeclaration config = (ConfigurationElementDeclaration) app.getGlobalElements().get(0);
    app.addGlobalElement(declareOtherConfig(config.getConnection().get(), "newName",
                                            PARAMETER_REQUIRED_FOR_METADATA_DEFAULT_VALUE,
                                            ACTING_PARAMETER_DEFAULT_VALUE,
                                            PROVIDED_PARAMETER_DEFAULT_VALUE,
                                            PARAMETER_IN_GROUP_DEFAULT_VALUE));
    Optional<ValueProviderCacheId> config1Id =
        computeIdFor(loadAst("differentConfigsWithSameParameterGetSameHash_1"), app, MY_CONFIG, PROVIDED_PARAMETER_NAME);
    Optional<ValueProviderCacheId> config2Id =
        computeIdFor(loadAst("differentConfigsWithSameParameterGetSameHash_1"), app, "newName", PROVIDED_PARAMETER_NAME);
    checkIdsAreEqual(config1Id, config2Id);
  }

  @Test
  public void differentValueProviderNameGetsSameHash() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    when(valueProviderModel.requiresConnection()).thenReturn(true);
    when(valueProviderModel.requiresConfiguration()).thenReturn(true);
    Optional<ValueProviderCacheId> opId1 =
        computeIdFor(loadAst("differentValueProviderNameGetsSameHash_1"), app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    when(valueProviderModel.getProviderName()).thenReturn("newValueProviderName");
    Optional<ValueProviderCacheId> opId2 =
        computeIdFor(loadAst("differentValueProviderNameGetsSameHash_2"), app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    checkIdsAreEqual(opId1, opId2);
  }

  @Test
  public void differentValueProviderIdGetsDifferentHash() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    when(valueProviderModel.requiresConnection()).thenReturn(true);
    when(valueProviderModel.requiresConfiguration()).thenReturn(true);
    Optional<ValueProviderCacheId> opId1 =
        computeIdFor(loadAst("differentValueProviderIdGetsDifferentHash_1"), app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    when(valueProviderModel.getProviderId()).thenReturn("newValueProviderId");
    Optional<ValueProviderCacheId> opId2 =
        computeIdFor(loadAst("differentValueProviderIdGetsDifferentHash_2"), app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    checkIdsAreDifferent(opId1, opId2);
  }

  @Test
  public void differentOperationsWithSameParametersGetsSameHash() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> opId1 =
        computeIdFor(loadAst("differentOperationsWithSameParametersGetsSameHash_1"), app, OPERATION_LOCATION,
                     PROVIDED_PARAMETER_NAME);
    Optional<ValueProviderCacheId> opId2 = computeIdFor(loadAst("differentOperationsWithSameParametersGetsSameHash_1"), app,
                                                        OTHER_OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    checkIdsAreEqual(opId1, opId2);
  }

  @Test
  public void differentHashForComplexActingParameterValue() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    List<Optional<ValueProviderCacheId>> allIds = new LinkedList<>();
    AtomicInteger index = new AtomicInteger();
    forAllComplexActingParameterChanges(app,
                                        OPERATION_LOCATION,
                                        COMPLEX_ACTING_PARAMETER_NAME,
                                        v -> allIds
                                            .add(computeIdFor(loadAst("differentHashForComplexActingParameterValue_"
                                                + index.incrementAndGet()),
                                                              app,
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

    ArtifactDeclaration app = getBaseApp();

    Optional<ValueProviderCacheId> operationId =
        computeIdFor(loadAst("extractionExpressionIsUsedForActingParameters_1"), app, OPERATION_LOCATION,
                     PROVIDED_PARAMETER_NAME);
    assertThat(operationId.isPresent(), is(true));
    modifyParameter(app, OPERATION_LOCATION, ACTING_PARAMETER_NAME, p -> p.setValue(ParameterSimpleValue.of("newValue")));
    checkIdsAreDifferent(operationId,
                         computeIdFor(loadAst("extractionExpressionIsUsedForActingParameters_2"), app, OPERATION_LOCATION,
                                      PROVIDED_PARAMETER_NAME));
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

    ArtifactDeclaration app = getBaseApp();

    Optional<ValueProviderCacheId> operationId =
        computeIdFor(loadAst("wholeParameterIsUsedIfExpressionPointsToField_1"), app, OPERATION_LOCATION,
                     PROVIDED_FROM_COMPLEX_PARAMETER_NAME);
    assertThat(operationId.isPresent(), is(true));
    // Modify a parameter that should not affect the hash
    modifyParameter(app, OPERATION_LOCATION, COMPLEX_ACTING_PARAMETER_NAME, p -> {
      Map<String, ParameterValue> complexDeclaration = new HashMap<>(((ParameterObjectValue) p.getValue()).getParameters());
      complexDeclaration.put("intParam", plain("999"));
      ((ParameterObjectValue) p.getValue()).setParameters(complexDeclaration);
    });
    checkIdsAreDifferent(operationId,
                         computeIdFor(loadAst("wholeParameterIsUsedIfExpressionPointsToField_2"), app, OPERATION_LOCATION,
                                      PROVIDED_PARAMETER_NAME));
  }

  @Test
  public void presentFieldValueProviderGetsId() throws Exception {
    final String targetSelector = "some.target.path";
    FieldValueProviderModel fieldValueProviderModel = createFieldValueProviderModel(FIELD_VALUE_PROVIDER_NAME,
                                                                                    FIELD_VALUE_PROVIDER_ID,
                                                                                    targetSelector);
    when(providedParameter.getFieldValueProviderModels()).thenReturn(singletonList(fieldValueProviderModel));

    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> cacheId =
        computeIdFor(loadAst("presentFieldValueProviderGetsId_1"), app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME,
                     targetSelector);
    assertThat(cacheId.isPresent(), equalTo(true));
    cacheId = computeIdFor(loadAst("presentFieldValueProviderGetsId_1"), app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME,
                           "other.field");
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

    ArtifactDeclaration app = getBaseApp();

    List<Optional<ValueProviderCacheId>> allIds = new LinkedList<>();
    AtomicInteger index = new AtomicInteger();
    forAllComplexActingParameterChanges(app, OPERATION_LOCATION, COMPLEX_ACTING_PARAMETER_NAME,
                                        v -> allIds
                                            .add(computeIdFor(loadAst("changesInParameterWithActingFieldReturnsDifferentHash_"
                                                + index.incrementAndGet()),
                                                              app,
                                                              OPERATION_LOCATION,
                                                              PROVIDED_FROM_COMPLEX_PARAMETER_NAME, targetSelector)));

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

    ArtifactDeclaration app = getBaseApp();

    Optional<ValueProviderCacheId> id = computeIdFor(loadAst("actingFieldFromNotExistentParameterIsNotConsideredForId_1"), app,
                                                     OPERATION_LOCATION, PROVIDED_PARAMETER_NAME, targetSelector);
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

    ArtifactDeclaration app = getBaseApp();

    modifyParameter(app, OPERATION_LOCATION, COMPLEX_ACTING_PARAMETER_NAME,
                    p -> p.setValue(ParameterSimpleValue.of("#['complexActingParameter']")));
    Optional<ValueProviderCacheId> originalExpressionId =
        computeIdFor(loadAst("actingFieldAsExpressionUsesWholeParameter_1"), app, OPERATION_LOCATION,
                     PROVIDED_FROM_COMPLEX_PARAMETER_NAME, targetSelector);

    modifyParameter(app, OPERATION_LOCATION, COMPLEX_ACTING_PARAMETER_NAME,
                    p -> p.setValue(ParameterSimpleValue.of("#['otherComplexActingParameter']")));
    Optional<ValueProviderCacheId> otherExpressionId =
        computeIdFor(loadAst("actingFieldAsExpressionUsesWholeParameter_2"), app, OPERATION_LOCATION,
                     PROVIDED_FROM_COMPLEX_PARAMETER_NAME, targetSelector);

    checkIdsAreDifferent(originalExpressionId, otherExpressionId);
  }

  @Test
  public void invalidTargetSelector() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    assertThat(computeIdFor(loadAst("invalidTargetSelector_1"), app, OPERATION_LOCATION,
                            PROVIDED_PARAMETER_NAME, "this-is-not&a$$$val*d@path")
                                .isPresent(),
               is(false));
  }
}
