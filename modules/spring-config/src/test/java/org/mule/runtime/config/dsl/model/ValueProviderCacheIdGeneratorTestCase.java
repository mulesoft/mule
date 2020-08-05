/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static junit.framework.TestCase.fail;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mule.runtime.app.declaration.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.runtime.internal.dsl.DslConstants.CONFIG_ATTRIBUTE_NAME;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.ConfigurationElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterizedElementDeclaration;
import org.mule.runtime.app.declaration.api.fluent.ParameterSimpleValue;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.config.api.dsl.model.metadata.ComponentAstBasedValueProviderCacheIdGenerator;
import org.mule.runtime.config.api.dsl.model.metadata.ContextBasedValueProviderCacheIdGenerator;
import org.mule.runtime.config.api.dsl.model.metadata.context.AstValueProviderCacheIdGeneratorContext;
import org.mule.runtime.config.api.dsl.model.metadata.context.DeclarationValueProviderCacheIdGeneratorContextFactory;
import org.mule.runtime.config.api.dsl.model.metadata.context.ValueProviderCacheIdGeneratorContext;
import org.mule.runtime.config.internal.model.ApplicationModel;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheId;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheIdGenerator;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.junit.Test;

public class ValueProviderCacheIdGeneratorTestCase extends AbstractMockedValueProviderExtensionTestCase {

  private ValueProviderCacheIdGenerator<ValueProviderCacheIdGeneratorContext<?>> contextBasedValueProviderCacheIdGenerator =
          new ContextBasedValueProviderCacheIdGenerator();
  private DeclarationValueProviderCacheIdGeneratorContextFactory declarationContextFactory;

  @Override
  public void before() {
    super.before();
    declarationContextFactory =
            new DeclarationValueProviderCacheIdGeneratorContextFactory(DslElementModelFactory.getDefault(dslContext));
  }

  private Optional<ValueProviderCacheId> computeIdFor(ArtifactDeclaration appDeclaration,
                                                                 String location,
                                                                 String parameterName)
      throws Exception {
    ApplicationModel app = loadApplicationModel(appDeclaration);
    Locator locator = new Locator(app);
    ValueProviderCacheIdGenerator<ComponentAst> componentAstBasedValueProviderCacheIdGenerator = new ComponentAstBasedValueProviderCacheIdGenerator(locator);
    ComponentAst component = getComponentAst(app, location);

    Optional<ComponentAst> configAst = resolveConfigName(component)
            .flatMap(configName -> locator.get(Location.builderFromStringRepresentation(configName).build()));

    ValueProviderCacheIdGeneratorContext astContext = configAst.map(
            c -> new AstValueProviderCacheIdGeneratorContext(component,
                                                             c))
            .orElse(new AstValueProviderCacheIdGeneratorContext(component));


    Optional<ParameterizedElementDeclaration> elementDeclaration = appDeclaration.findElement(builderFromStringRepresentation(location).build());
    Optional<ParameterizedModel> elementModel = component.getModel(ParameterizedModel.class);

    if (!elementDeclaration.isPresent() || !elementModel.isPresent()) {
      fail(format("missing declaration or model for: %s", location));
    }


    ValueProviderCacheIdGeneratorContext declarationContext = configAst
            .map(
                    c -> {
                      final Optional<ConfigurationElementDeclaration> configDeclaration =
                              appDeclaration.findElement(builderFromStringRepresentation(c.getLocation().getLocation()).build());
                      final Optional<ConfigurationModel> configModel = c.getModel(ConfigurationModel.class);
                      if (!configDeclaration.isPresent() || !configModel.isPresent()) {
                        fail(format("missing declaration or model for config: %s", c.getLocation().getLocation()));
                      }
                      return declarationContextFactory.createContext(elementDeclaration.get(), elementModel.get(),
                                                                     configDeclaration.get(), configModel.get());
                    })
            .orElse(declarationContextFactory.createContext(elementDeclaration.get(), elementModel.get()));

    Optional<ValueProviderCacheId> astContextId = contextBasedValueProviderCacheIdGenerator.getIdForResolvedValues(astContext, parameterName);
    Optional<ValueProviderCacheId> componentBasedId = componentAstBasedValueProviderCacheIdGenerator.getIdForResolvedValues(component, parameterName);
    Optional<ValueProviderCacheId> declarationContextId = contextBasedValueProviderCacheIdGenerator.getIdForResolvedValues(declarationContext, parameterName);

    checkIdsAreEqual(astContextId, componentBasedId);
    checkIdsAreEqual(componentBasedId, declarationContextId);

    //Any should be fine
    return declarationContextId;
  }

  private Optional<String> resolveConfigName(ComponentAst elementModel) {
    return elementModel.getRawParameterValue(CONFIG_ATTRIBUTE_NAME);
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
    when(configuration.getConnectionProviderModel(anyString())).thenReturn(Optional.of(connectionProvider));
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
    when(configuration.getConnectionProviderModel(anyString())).thenReturn(Optional.of(connectionProvider));
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

  @Test
  public void differentHashForComplexActingParameterValue() throws Exception {
    ArtifactDeclaration app = getBaseApp();
    int intParam = 0;
    String stringParam = "zero";
    List<String> listParam = asList("one", "two", "three");
    int innerIntParam = 0;
    String innerStringParam = "zero";
    List<String> innerListParm = asList("one", "two", "three");
    Optional<ValueProviderCacheId> originalId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_FROM_COMPLEX_PARAMETER_NAME);

    modifyParameter(app, OPERATION_LOCATION, COMPLEX_ACTING_PARAMETER_NAME, p -> p.setValue(newComplexActingParameter(1,
                                                                                                                      stringParam,
                                                                                                                      listParam,
                                                                                                                      innerIntParam,
                                                                                                                      innerStringParam,
                                                                                                                      innerListParm)));
    Optional<ValueProviderCacheId> changedIntId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_FROM_COMPLEX_PARAMETER_NAME);

    modifyParameter(app, OPERATION_LOCATION, COMPLEX_ACTING_PARAMETER_NAME, p -> p.setValue(newComplexActingParameter(intParam,
                                                                                                                      "one",
                                                                                                                      listParam,
                                                                                                                      innerIntParam,
                                                                                                                      innerStringParam,
                                                                                                                      innerListParm)));
    Optional<ValueProviderCacheId> changedStringId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_FROM_COMPLEX_PARAMETER_NAME);

    modifyParameter(app, OPERATION_LOCATION, COMPLEX_ACTING_PARAMETER_NAME, p -> p.setValue(newComplexActingParameter(intParam,
                                                                                                                      stringParam,
                                                                                                                      asList("one", "two", "four"),
                                                                                                                      innerIntParam,
                                                                                                                      innerStringParam,
                                                                                                                      innerListParm)));
    Optional<ValueProviderCacheId> changedListId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_FROM_COMPLEX_PARAMETER_NAME);

    modifyParameter(app, OPERATION_LOCATION, COMPLEX_ACTING_PARAMETER_NAME, p -> p.setValue(newComplexActingParameter(intParam,
                                                                                                                      stringParam,
                                                                                                                      listParam,
                                                                                                                      1,
                                                                                                                      innerStringParam,
                                                                                                                      innerListParm)));
    Optional<ValueProviderCacheId> changedInnerIntId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_FROM_COMPLEX_PARAMETER_NAME);

    modifyParameter(app, OPERATION_LOCATION, COMPLEX_ACTING_PARAMETER_NAME, p -> p.setValue(newComplexActingParameter(intParam,
                                                                                                                      stringParam,
                                                                                                                      listParam,
                                                                                                                      innerIntParam,
                                                                                                                      "one",
                                                                                                                      innerListParm)));
    Optional<ValueProviderCacheId> changedInnerStringId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_FROM_COMPLEX_PARAMETER_NAME);

    modifyParameter(app, OPERATION_LOCATION, COMPLEX_ACTING_PARAMETER_NAME, p -> p.setValue(newComplexActingParameter(intParam,
                                                                                                                      stringParam,
                                                                                                                      listParam,
                                                                                                                      innerIntParam,
                                                                                                                      innerStringParam,
                                                                                                                      asList("one", "two", "four"))));
    Optional<ValueProviderCacheId> changedInnerListId = computeIdFor(app, OPERATION_LOCATION, PROVIDED_FROM_COMPLEX_PARAMETER_NAME);

    List<Optional<ValueProviderCacheId>> allIds = asList(originalId, changedIntId, changedStringId, changedListId, changedInnerIntId, changedInnerStringId, changedInnerListId);
    for(Optional<ValueProviderCacheId> idA : allIds) {
      for(Optional<ValueProviderCacheId> idB : allIds) {
        if(idA != idB) {
          checkIdsAreDifferent(idA, idB);
        }
      }
    }
  }

}
