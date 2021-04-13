/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.mule.runtime.app.declaration.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.runtime.config.dsl.model.ComplexActingParameterUtils.DEFAULT_COMPLEX_ACTING_PARAMETER;
import static org.mule.runtime.config.dsl.model.ComplexActingParameterUtils.forAllComplexActingParameterChanges;
import static org.mule.runtime.config.dsl.model.DeclarationUtils.modifyParameter;
import static org.mule.runtime.config.dsl.model.DeclarationUtils.removeParameter;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.meta.model.parameter.ActingParameterModel;
import org.mule.runtime.api.meta.model.parameter.FieldValueProviderModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.ConfigurationElementDeclaration;
import org.mule.runtime.app.declaration.api.ElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterizedElementDeclaration;
import org.mule.runtime.app.declaration.api.fluent.ParameterSimpleValue;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.api.dsl.model.DslElementModel;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.config.api.dsl.model.metadata.ComponentAstBasedValueProviderCacheIdGenerator;
import org.mule.runtime.config.api.dsl.model.metadata.ComponentBasedValueProviderCacheIdGenerator;
import org.mule.runtime.config.api.dsl.model.metadata.DeclarationBasedValueProviderCacheIdGenerator;
import org.mule.runtime.config.api.dsl.model.metadata.DslElementBasedValueProviderCacheIdGenerator;
import org.mule.runtime.core.internal.locator.ComponentLocator;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheId;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheIdGenerator;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

public class ValueProviderCacheIdGeneratorTestCase extends AbstractMockedValueProviderExtensionTestCase {

  private DslElementModelFactory dslElementModelFactory;

  @Override
  public void before() {
    super.before();
    dslElementModelFactory = DslElementModelFactory.getDefault(dslContext);
  }

  private Optional<ValueProviderCacheId> computeIdFor(ArtifactDeclaration appDeclaration,
                                                      String location,
                                                      String parameterName)
          throws Exception {
    return this.computeIdFor(appDeclaration, location, parameterName, null);
  }

  private Optional<ValueProviderCacheId> computeIdFor(ArtifactDeclaration appDeclaration,
                                                      String location,
                                                      String parameterName,
                                                      String targetPath)
          throws Exception {
    ArtifactAst app = loadApplicationModel(appDeclaration);
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

    Optional<ValueProviderCacheId> dslElementId =
        dslElementModelValueProviderCacheIdGenerator.getIdForResolvedValues(dslElementModel, parameterName);
    Optional<ValueProviderCacheId> componentBasedId =
        componentBasedValueProviderCacheIdGenerator.getIdForResolvedValues(component, parameterName);
    Optional<ValueProviderCacheId> declarationBasedId =
        elementDeclarationValueProviderCacheIdGenerator.getIdForResolvedValues(elementDeclaration.get(), parameterName);
    Optional<ValueProviderCacheId> astId =
        componentAstBasedValueProviderCacheIdGenerator.getIdForResolvedValues(component, parameterName);
    Optional<ValueProviderCacheId> dslElementId;
    Optional<ValueProviderCacheId> componentBasedId;
    Optional<ValueProviderCacheId> declarationBasedId;
    if (targetPath == null) {
      dslElementId = dslElementModelValueProviderCacheIdGenerator.getIdForResolvedValues(dslElementModel, parameterName);
      componentBasedId = componentBasedValueProviderCacheIdGenerator.getIdForResolvedValues(component, parameterName);
      declarationBasedId =
              elementDeclarationValueProviderCacheIdGenerator.getIdForResolvedValues(elementDeclaration.get(), parameterName);
    }
    else {
      dslElementId =
              dslElementModelValueProviderCacheIdGenerator.getIdForResolvedValues(dslElementModel, parameterName, targetPath);
      componentBasedId = componentBasedValueProviderCacheIdGenerator.getIdForResolvedValues(component, parameterName, targetPath);
      declarationBasedId = elementDeclarationValueProviderCacheIdGenerator.getIdForResolvedValues(elementDeclaration.get(),
                                                                                                  parameterName, targetPath);
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
  public void idForConfiglessAndConnectionlessOperationDefaultValueHashIdShouldBeSameWithExplicitValueOnActingParameter()
          throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> opId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    assertThat(opId.isPresent(), is(true));
    removeParameter(app, OPERATION_LOCATION, ACTING_PARAMETER_NAME);
    checkIdsAreEqual(opId, computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME));
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
  public void differentConfigsWithSameParameterGetSameHash() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    ConfigurationElementDeclaration config = (ConfigurationElementDeclaration) app.getGlobalElements().get(0);
    app.addGlobalElement(declareOtherConfig(config.getConnection().get(), "newName",
                                            PARAMETER_REQUIRED_FOR_METADATA_DEFAULT_VALUE,
                                            ACTING_PARAMETER_DEFAULT_VALUE,
                                            PROVIDED_PARAMETER_DEFAULT_VALUE,
                                            PARAMETER_IN_GROUP_DEFAULT_VALUE));
    Optional<ValueProviderCacheId> config1Id = computeIdFor(app, MY_CONFIG, PROVIDED_PARAMETER_NAME);
    Optional<ValueProviderCacheId> config2Id = computeIdFor(app, "newName", PROVIDED_PARAMETER_NAME);
    checkIdsAreEqual(config1Id, config2Id);
  }

  @Test
  public void differentValueProviderNameGetsSameHash() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    when(valueProviderModel.requiresConnection()).thenReturn(true);
    when(valueProviderModel.requiresConfiguration()).thenReturn(true);
    Optional<ValueProviderCacheId> opId1 = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    when(valueProviderModel.getProviderName()).thenReturn("newValueProviderName");
    Optional<ValueProviderCacheId> opId2 = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    checkIdsAreEqual(opId1, opId2);
  }

  @Test
  public void differentValueProviderIdGetsDifferentHash() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    when(valueProviderModel.requiresConnection()).thenReturn(true);
    when(valueProviderModel.requiresConfiguration()).thenReturn(true);
    Optional<ValueProviderCacheId> opId1 = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    when(valueProviderModel.getProviderId()).thenReturn("newValueProviderId");
    Optional<ValueProviderCacheId> opId2 = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    checkIdsAreDifferent(opId1, opId2);
  }

  @Test
  public void differentOperationsWithSameParametersGetsSameHash() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> opId1 = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    Optional<ValueProviderCacheId> opId2 = computeIdFor(app, OTHER_OPERATION_LOCATION, PROVIDED_PARAMETER_NAME);
    checkIdsAreEqual(opId1, opId2);
  }

  @Test
  public void differentHashForComplexActingParameterValue() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    List<Optional<ValueProviderCacheId>> allIds = new LinkedList<>();
    forAllComplexActingParameterChanges(app,
                                        OPERATION_LOCATION,
                                        COMPLEX_ACTING_PARAMETER_NAME,
                                        v -> allIds
                                                .add(computeIdFor(app, OPERATION_LOCATION, PROVIDED_FROM_COMPLEX_PARAMETER_NAME)));

    for (Optional<ValueProviderCacheId> idA : allIds) {
      for (Optional<ValueProviderCacheId> idB : allIds) {
        if (idA != idB) {
          checkIdsAreDifferent(idA, idB);
        }
      }
    }
  }

  @Test
  public void presentFieldValueProviderGetsId() throws Exception {
    final String targetPath = "some.target.path";
    FieldValueProviderModel fieldValueProviderModel = createFieldValueProviderModel(FIELD_VALUE_PROVIDER_NAME,
                                                                                    FIELD_VALUE_PROVIDER_ID,
                                                                                    targetPath);
    when(providedParameter.getFieldValueProviderModels()).thenReturn(singletonList(fieldValueProviderModel));

    ArtifactDeclaration app = getBaseApp();
    Optional<ValueProviderCacheId> cacheId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME, targetPath);
    assertThat(cacheId.isPresent(), equalTo(true));
    cacheId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_PARAMETER_NAME, "other.field");
    assertThat(cacheId.isPresent(), is(false));
  }

  @Test
  public void nonActingFieldReturnsSameHash() throws Exception {
    final String targetPath = "some.target.path";
    FieldValueProviderModel fieldValueProviderModel =
            createFieldValueProviderModel(FIELD_VALUE_PROVIDER_NAME, FIELD_VALUE_PROVIDER_ID, targetPath);

    ActingParameterModel actingParameterModel = createActingParameterModel(COMPLEX_ACTING_PARAMETER_NAME,
                                                                           COMPLEX_ACTING_PARAMETER_NAME
                                                                           + ".innerPojoParam.stringParam");

    when(fieldValueProviderModel.getParameters()).thenReturn(singletonList(actingParameterModel));
    when(providedParameterFromComplex.getFieldValueProviderModels()).thenReturn(singletonList(fieldValueProviderModel));

    ArtifactDeclaration app = getBaseApp();

    List<Optional<ValueProviderCacheId>> allIds = new LinkedList<>();
    Reference<Optional<ValueProviderCacheId>> modifiedActingFieldIdReference = new Reference<>();

    // We only care that the ValueProviderCacheId changes when the defined acting field changes (innerPojoParam.stringParam)
    // Then, every other computed id should be the same
    forAllComplexActingParameterChanges(app, OPERATION_LOCATION, COMPLEX_ACTING_PARAMETER_NAME,
                                        v -> {
                                          Optional<ValueProviderCacheId> id =
                                                  computeIdFor(app, OPERATION_LOCATION, PROVIDED_FROM_COMPLEX_PARAMETER_NAME,
                                                               targetPath);
                                          if (v.getInnerPojoParam().getStringParam()
                                                  .equals(DEFAULT_COMPLEX_ACTING_PARAMETER.getInnerPojoParam().getStringParam())) {
                                            allIds.add(id);
                                          }
                                          else {
                                            modifiedActingFieldIdReference.set(id);
                                          }
                                        });
    Optional<ValueProviderCacheId> modifiedActingFieldId = modifiedActingFieldIdReference.get();
    assertThat(modifiedActingFieldId.isPresent(), is(true));

    Optional<ValueProviderCacheId> listId = allIds.get(0);

    // Every id in the list should be equal to each other and all should be different to the modified acting field id
    allIds.forEach(
            id -> {
              checkIdsAreEqual(id, listId);
              checkIdsAreDifferent(id, modifiedActingFieldId);
            });
  }

  @Test
  public void actingFieldAsExpressionUsesWholeParameter() throws Exception {
    final String targetPath = "some.target.path";
    FieldValueProviderModel fieldValueProviderModel =
            createFieldValueProviderModel(FIELD_VALUE_PROVIDER_NAME, FIELD_VALUE_PROVIDER_ID, targetPath);

    ActingParameterModel actingParameterModel = createActingParameterModel(COMPLEX_ACTING_PARAMETER_NAME,
                                                                           COMPLEX_ACTING_PARAMETER_NAME
                                                                           + ".innerPojoParam.stringParam");

    when(fieldValueProviderModel.getParameters()).thenReturn(singletonList(actingParameterModel));
    when(providedParameterFromComplex.getFieldValueProviderModels()).thenReturn(singletonList(fieldValueProviderModel));

    ArtifactDeclaration app = getBaseApp();

    modifyParameter(app, OPERATION_LOCATION, COMPLEX_ACTING_PARAMETER_NAME, p -> p.setValue(ParameterSimpleValue.of("#['complexActingParameter']")));
    Optional<ValueProviderCacheId> originalExpressionId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_FROM_COMPLEX_PARAMETER_NAME, targetPath);

    modifyParameter(app, OPERATION_LOCATION, COMPLEX_ACTING_PARAMETER_NAME, p -> p.setValue(ParameterSimpleValue.of("#['otherComplexActingParameter']")));
    Optional<ValueProviderCacheId> otherExpressionId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_FROM_COMPLEX_PARAMETER_NAME, targetPath);

    checkIdsAreDifferent(originalExpressionId, otherExpressionId);
  }

  @Test
  public void invalidTargetPath() throws Exception {
    final String targetPath = "some.target.path";

    FieldValueProviderModel fieldValueProviderModel =
            createFieldValueProviderModel(FIELD_VALUE_PROVIDER_NAME, FIELD_VALUE_PROVIDER_ID, targetPath);

    ActingParameterModel actingParameterModel = createActingParameterModel(COMPLEX_ACTING_PARAMETER_NAME,
                                                                           COMPLEX_ACTING_PARAMETER_NAME
                                                                           + ".innerPojoParam.stringParam");

    when(fieldValueProviderModel.getParameters()).thenReturn(singletonList(actingParameterModel));
    when(providedParameterFromComplex.getFieldValueProviderModels()).thenReturn(singletonList(fieldValueProviderModel));

    ArtifactDeclaration app = getBaseApp();
    assertThat(computeIdFor(app, OPERATION_LOCATION, PROVIDED_FROM_COMPLEX_PARAMETER_NAME, "this-is-not&a$$$val*d@path").isPresent(), is(false));
  }

  @Test
  public void attributeInPathNotConsideredIfElement() throws Exception {
    final String targetPath = "some.target.path";
    FieldValueProviderModel fieldValueProviderModel =
            createFieldValueProviderModel(FIELD_VALUE_PROVIDER_NAME, FIELD_VALUE_PROVIDER_ID, targetPath);

    ActingParameterModel actingParameterModel = createActingParameterModel(COMPLEX_ACTING_PARAMETER_NAME,
                                                                           COMPLEX_ACTING_PARAMETER_NAME
                                                                           + ".@innerPojoParam");

    when(fieldValueProviderModel.getParameters()).thenReturn(singletonList(actingParameterModel));
    when(providedParameterFromComplex.getFieldValueProviderModels()).thenReturn(singletonList(fieldValueProviderModel));

    ArtifactDeclaration app = getBaseApp();

    List<Optional<ValueProviderCacheId>> allIds = new LinkedList<>();
    forAllComplexActingParameterChanges(app,
                                        OPERATION_LOCATION,
                                        COMPLEX_ACTING_PARAMETER_NAME,
                                        c -> allIds.add(computeIdFor(app,
                                                                     OPERATION_LOCATION,
                                                                     PROVIDED_FROM_COMPLEX_PARAMETER_NAME,
                                                                     targetPath)));

    Optional<ValueProviderCacheId> listId = allIds.get(0);

    // Every id in the list should be equal because complexActingParameter.@innerPojoParam does not exist since innerPojoParam is not an attribute
    allIds.forEach(id -> checkIdsAreEqual(id, listId));
  }

  @Test
  public void attributeInConsideredIfAttribute() throws Exception {
    final String targetPath = "some.target.path";
    FieldValueProviderModel fieldValueProviderModel =
            createFieldValueProviderModel(FIELD_VALUE_PROVIDER_NAME, FIELD_VALUE_PROVIDER_ID, targetPath);

    ActingParameterModel actingParameterModel = createActingParameterModel(COMPLEX_ACTING_PARAMETER_NAME,
                                                                           COMPLEX_ACTING_PARAMETER_NAME
                                                                           + ".innerPojoParam.@stringParam");

    when(fieldValueProviderModel.getParameters()).thenReturn(singletonList(actingParameterModel));
    when(providedParameterFromComplex.getFieldValueProviderModels()).thenReturn(singletonList(fieldValueProviderModel));

    ArtifactDeclaration app = getBaseApp();

    List<Optional<ValueProviderCacheId>> allIds = new LinkedList<>();
    Reference<Optional<ValueProviderCacheId>> changedStringParamReference = new Reference<>();
    forAllComplexActingParameterChanges(app,
                                        OPERATION_LOCATION,
                                        COMPLEX_ACTING_PARAMETER_NAME,
                                        c -> {
                                          Optional<ValueProviderCacheId> id = computeIdFor(app,
                                                                                           OPERATION_LOCATION,
                                                                                           PROVIDED_FROM_COMPLEX_PARAMETER_NAME,
                                                                                           targetPath);
                                          if (c.getInnerPojoParam().getStringParam().equals(DEFAULT_COMPLEX_ACTING_PARAMETER.getInnerPojoParam().getStringParam())) {
                                            allIds.add(id);
                                          }
                                          else {
                                            changedStringParamReference.set(id);
                                          }
                                        });

    Optional<ValueProviderCacheId> listId = allIds.get(0);

    // Every id in the list should be equal to each other and all should be different to the modified acting field id
    allIds.forEach(
            id -> {
              checkIdsAreEqual(id, listId);
              checkIdsAreDifferent(id, changedStringParamReference.get());
            });
  }


}
