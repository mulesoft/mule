/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model;

import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.CONFIG;
import static org.mule.runtime.ast.api.util.MuleAstUtils.createComponentParameterizationFromComponentAst;

import static java.util.stream.Collectors.toList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.parameterization.ComponentParameterization;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider;
import org.mule.runtime.metadata.api.cache.ConfigurationMetadataCacheIdGenerator;
import org.mule.runtime.metadata.api.cache.MetadataCacheId;
import org.mule.runtime.metadata.api.cache.MetadataCacheIdGenerator;
import org.mule.runtime.metadata.api.locator.ComponentLocator;
import org.mule.runtime.metadata.internal.cache.AstConfigurationMetadataCacheIdGenerator;
import org.mule.runtime.metadata.internal.cache.ComponentAstBasedMetadataCacheIdGenerator;
import org.mule.runtime.metadata.internal.cache.ComponentParameterizationBasedMetadataCacheIdGenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import org.junit.Before;

public abstract class AbstractMetadataCacheIdGeneratorTestCase extends AbstractDslModelTestCase {

  protected Set<ExtensionModel> extensions;
  protected DslResolvingContext dslResolvingContext;

  @Before
  public void setUp() throws Exception {
    extensions = ImmutableSet.<ExtensionModel>builder()
        .add(MuleExtensionModelProvider.getExtensionModel())
        .add(mockExtension)
        .build();

    dslResolvingContext = DslResolvingContext.getDefault(extensions);
  }

  protected MetadataCacheId getIdForComponentOutputMetadata(ArtifactAst app, String location)
      throws Exception {
    ComponentAst component = new Locator(app)
        .get(Location.builderFromStringRepresentation(location).build())
        .get();

    MetadataCacheId astBasedId = createAstBasedGenerator(app).getIdForComponentOutputMetadata(component).get();
    MetadataCacheId parameterizationBasedId = createComponentParameterizationBasedGenerator(app)
        .getIdForComponentOutputMetadata(createComponentParameterizationFromComponentAst(component)).get();

    assertThat(parameterizationBasedId, equalTo(astBasedId));

    // Any should be fine
    return astBasedId;
  }

  protected MetadataCacheId getIdForComponentAttributesMetadata(ArtifactAst app, String location)
      throws Exception {
    ComponentAst component = new Locator(app)
        .get(Location.builderFromStringRepresentation(location).build())
        .get();

    MetadataCacheId astBasedId = createAstBasedGenerator(app).getIdForComponentAttributesMetadata(component).get();
    MetadataCacheId parameterizationBasedId = createComponentParameterizationBasedGenerator(app)
        .getIdForComponentAttributesMetadata(createComponentParameterizationFromComponentAst(component)).get();

    assertThat(parameterizationBasedId, equalTo(astBasedId));

    // Any should be fine
    return astBasedId;
  }

  protected MetadataCacheId getIdForComponentInputMetadata(ArtifactAst app, String location, String parameterName)
      throws Exception {
    ComponentAst component = new Locator(app)
        .get(Location.builderFromStringRepresentation(location).build())
        .get();

    MetadataCacheId astBasedId = createAstBasedGenerator(app).getIdForComponentInputMetadata(component, parameterName).get();
    MetadataCacheId parameterizationBasedId = createComponentParameterizationBasedGenerator(app)
        .getIdForComponentInputMetadata(createComponentParameterizationFromComponentAst(component), parameterName).get();

    assertThat(parameterizationBasedId, equalTo(astBasedId));

    // Any should be fine
    return astBasedId;
  }

  protected MetadataCacheId getIdForComponentMetadata(ArtifactAst app, String location)
      throws Exception {
    ComponentAst component = new Locator(app)
        .get(Location.builderFromStringRepresentation(location).build())
        .get();

    MetadataCacheId astBasedId = createAstBasedGenerator(app).getIdForComponentMetadata(component).get();
    MetadataCacheId parameterizationBasedId = createComponentParameterizationBasedGenerator(app)
        .getIdForComponentMetadata(createComponentParameterizationFromComponentAst(component)).get();

    assertThat(parameterizationBasedId, equalTo(astBasedId));

    // Any should be fine
    return astBasedId;
  }

  protected MetadataCacheId getIdForMetadataKeys(ArtifactAst app, String location)
      throws Exception {
    ComponentAst component = new Locator(app)
        .get(Location.builderFromStringRepresentation(location).build())
        .get();

    MetadataCacheId astBasedId = createAstBasedGenerator(app).getIdForMetadataKeys(component).get();
    MetadataCacheId parameterizationBasedId = createComponentParameterizationBasedGenerator(app)
        .getIdForMetadataKeys(createComponentParameterizationFromComponentAst(component)).get();

    assertThat(parameterizationBasedId, equalTo(astBasedId));

    // Any should be fine
    return astBasedId;
  }

  protected MetadataCacheId getIdForGlobalMetadata(ArtifactAst app, String location)
      throws Exception {
    ComponentAst component = new Locator(app)
        .get(Location.builderFromStringRepresentation(location).build())
        .get();

    MetadataCacheId astBasedId = createAstBasedGenerator(app).getIdForGlobalMetadata(component).get();
    MetadataCacheId parameterizationBasedId = createComponentParameterizationBasedGenerator(app)
        .getIdForGlobalMetadata(createComponentParameterizationFromComponentAst(component)).get();

    assertThat(parameterizationBasedId, equalTo(astBasedId));

    // Any should be fine
    return astBasedId;
  }

  private MetadataCacheIdGenerator<ComponentAst> createAstBasedGenerator(ArtifactAst app) {
    return new ComponentAstBasedMetadataCacheIdGenerator(new Locator(app));
  }

  private MetadataCacheIdGenerator<ComponentParameterization<?>> createComponentParameterizationBasedGenerator(ArtifactAst app) {
    ConfigurationMetadataCacheIdGenerator configGenerator = new AstConfigurationMetadataCacheIdGenerator();
    configGenerator.addConfigurations(app.topLevelComponentsStream()
        .filter(potentialConfig -> potentialConfig.getComponentType().equals(CONFIG)).collect(toList()));
    return new ComponentParameterizationBasedMetadataCacheIdGenerator(configGenerator);
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
