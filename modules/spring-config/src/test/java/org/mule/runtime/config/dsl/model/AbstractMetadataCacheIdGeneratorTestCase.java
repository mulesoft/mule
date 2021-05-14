/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mule.runtime.app.declaration.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.runtime.config.api.dsl.ArtifactDeclarationUtils.toArtifactast;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.ElementDeclaration;
import org.mule.runtime.app.declaration.api.fluent.ElementDeclarer;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.api.dsl.model.DslElementModel;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.config.api.dsl.model.metadata.DeclarationBasedMetadataCacheIdGenerator;
import org.mule.runtime.config.api.dsl.model.metadata.DslElementBasedMetadataCacheIdGenerator;
import org.mule.runtime.config.api.dsl.model.metadata.ModelBasedMetadataCacheIdGeneratorFactory;
import org.mule.runtime.core.api.extension.MuleExtensionModelProvider;
import org.mule.runtime.core.internal.locator.ComponentLocator;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheId;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheIdGenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;

import com.google.common.collect.ImmutableSet;

public abstract class AbstractMetadataCacheIdGeneratorTestCase extends AbstractDslModelTestCase {

  protected Set<ExtensionModel> extensions;
  protected DslResolvingContext dslResolvingContext;
  protected ElementDeclarer declarer;
  protected DslElementModelFactory dslFactory;

  @Before
  public void setUp() throws Exception {
    extensions = ImmutableSet.<ExtensionModel>builder()
        .add(MuleExtensionModelProvider.getExtensionModel())
        .add(mockExtension)
        .build();

    dslResolvingContext = DslResolvingContext.getDefault(extensions);
    declarer = ElementDeclarer.forExtension(EXTENSION_NAME);
    dslFactory = DslElementModelFactory.getDefault(dslResolvingContext);
  }

  protected MetadataCacheId getIdForComponentOutputMetadata(ArtifactDeclaration declaration, String location) throws Exception {
    ArtifactAst app = loadApplicationModel(declaration);
    ComponentAst component = new Locator(app)
        .get(Location.builderFromStringRepresentation(location).build())
        .get();

    ElementDeclaration elementDeclaration = declaration.findElement(builderFromStringRepresentation(location).build()).get();

    // MetadataCacheId astBasedId = createAstBasedGenerator(app).getIdForComponentOutputMetadata(component).get();
    MetadataCacheId modelBasedId = createModelBasedGenerator(app).getIdForComponentOutputMetadata(component).get();
    MetadataCacheId dslBasedId =
        createDslBasedGenerator(app, dslFactory).getIdForComponentOutputMetadata(dslFactory.create(component).get()).get();
    // MetadataCacheId declarationBasedId =
    // createDeclarationBasedGenerator(declaration).getIdForComponentOutputMetadata(elementDeclaration).get();

    assertThat(modelBasedId, equalTo(dslBasedId));
    // TODO: ADD THIS CHECK MULE-18685
    // assertThat(dslBasedId, equalTo(declarationBasedId));

    // TODO: ADD THIS CHECK MULE-18636
    // assertThat(declarationBasedId, astBasedId);

    // Any should be fine
    return modelBasedId;
  }

  protected MetadataCacheId getIdForComponentAttributesMetadata(ArtifactDeclaration declaration, String location)
      throws Exception {
    ArtifactAst app = loadApplicationModel(declaration);
    ComponentAst component = new Locator(app)
        .get(Location.builderFromStringRepresentation(location).build())
        .get();

    ElementDeclaration elementDeclaration = declaration.findElement(builderFromStringRepresentation(location).build()).get();

    // MetadataCacheId astBasedId = createAstBasedGenerator(app).getIdForComponentAttributesMetadata(component).get();
    MetadataCacheId modelBasedId = createModelBasedGenerator(app).getIdForComponentAttributesMetadata(component).get();
    MetadataCacheId dslBasedId =
        createDslBasedGenerator(app, dslFactory).getIdForComponentAttributesMetadata(dslFactory.create(component).get()).get();
    // MetadataCacheId declarationBasedId =
    // createDeclarationBasedGenerator(declaration).getIdForComponentAttributesMetadata(elementDeclaration).get();

    assertThat(modelBasedId, equalTo(dslBasedId));

    // TODO: ADD THIS CHECK MULE-18685
    // assertThat(dslBasedId, equalTo(declarationBasedId));

    // TODO: ADD THIS CHECK MULE-18636
    // assertThat(declarationBasedId, astBasedId);

    // Any should be fine
    return modelBasedId;
  }

  protected MetadataCacheId getIdForComponentInputMetadata(ArtifactDeclaration declaration, String location, String parameterName)
      throws Exception {
    ArtifactAst app = loadApplicationModel(declaration);
    ComponentAst component = new Locator(app)
        .get(Location.builderFromStringRepresentation(location).build())
        .get();

    ElementDeclaration elementDeclaration = declaration.findElement(builderFromStringRepresentation(location).build()).get();

    // MetadataCacheId astBasedId = createAstBasedGenerator(app).getIdForComponentInputMetadata(component).get();
    MetadataCacheId modelBasedId = createModelBasedGenerator(app).getIdForComponentInputMetadata(component, parameterName).get();
    MetadataCacheId dslBasedId = createDslBasedGenerator(app, dslFactory)
        .getIdForComponentInputMetadata(dslFactory.create(component).get(), parameterName).get();
    // MetadataCacheId declarationBasedId =
    // createDeclarationBasedGenerator(declaration).getIdForComponentInputMetadata(elementDeclaration, parameterName).get();

    assertThat(modelBasedId, equalTo(dslBasedId));

    // TODO: ADD THIS CHECK MULE-18685
    // assertThat(dslBasedId, equalTo(declarationBasedId));

    // TODO: ADD THIS CHECK MULE-18636
    // assertThat(declarationBasedId, astBasedId);

    // Any should be fine
    return modelBasedId;
  }

  protected MetadataCacheId getIdForComponentMetadata(ArtifactDeclaration declaration, String location) throws Exception {
    ArtifactAst app = loadApplicationModel(declaration);
    ComponentAst component = new Locator(app)
        .get(Location.builderFromStringRepresentation(location).build())
        .get();

    ElementDeclaration elementDeclaration = declaration.findElement(builderFromStringRepresentation(location).build()).get();

    // MetadataCacheId astBasedId = createAstBasedGenerator(app).getIdForComponentMetadata(component).get();
    MetadataCacheId modelBasedId = createModelBasedGenerator(app).getIdForComponentMetadata(component).get();
    MetadataCacheId dslBasedId =
        createDslBasedGenerator(app, dslFactory).getIdForComponentMetadata(dslFactory.create(component).get()).get();
    // MetadataCacheId declarationBasedId =
    // createDeclarationBasedGenerator(declaration).getIdForComponentMetadata(elementDeclaration).get();

    assertThat(modelBasedId, equalTo(dslBasedId));

    // TODO: ADD THIS CHECK MULE-18685
    // assertThat(dslBasedId, equalTo(declarationBasedId));

    // TODO: ADD THIS CHECK MULE-18636
    // assertThat(declarationBasedId, astBasedId);

    // Any should be fine
    return modelBasedId;
  }

  protected MetadataCacheId getIdForMetadataKeys(ArtifactDeclaration declaration, String location) throws Exception {
    ArtifactAst app = loadApplicationModel(declaration);
    ComponentAst component = new Locator(app)
        .get(Location.builderFromStringRepresentation(location).build())
        .get();

    ElementDeclaration elementDeclaration = declaration.findElement(builderFromStringRepresentation(location).build()).get();

    // MetadataCacheId astBasedId = createAstBasedGenerator(app).getIdForMetadataKeys(component).get();
    MetadataCacheId modelBasedId = createModelBasedGenerator(app).getIdForMetadataKeys(component).get();
    MetadataCacheId dslBasedId =
        createDslBasedGenerator(app, dslFactory).getIdForMetadataKeys(dslFactory.create(component).get()).get();
    // MetadataCacheId declarationBasedId =
    // createDeclarationBasedGenerator(declaration).getIdForMetadataKeys(elementDeclaration).get();

    assertThat(modelBasedId, equalTo(dslBasedId));

    // TODO: ADD THIS CHECK MULE-18685
    // assertThat(dslBasedId, equalTo(declarationBasedId));

    // TODO: ADD THIS CHECK MULE-18636
    // assertThat(declarationBasedId, astBasedId);

    // Any should be fine
    return modelBasedId;
  }

  protected MetadataCacheId getIdForGlobalMetadata(ArtifactDeclaration declaration, String location) throws Exception {
    ArtifactAst app = loadApplicationModel(declaration);
    ComponentAst component = new Locator(app)
        .get(Location.builderFromStringRepresentation(location).build())
        .get();

    ElementDeclaration elementDeclaration = declaration.findElement(builderFromStringRepresentation(location).build()).get();

    // MetadataCacheId astBasedId = createAstBasedGenerator(app).getIdForGlobalMetadata(component).get();
    MetadataCacheId modelBasedId = createModelBasedGenerator(app).getIdForGlobalMetadata(component).get();
    MetadataCacheId dslBasedId =
        createDslBasedGenerator(app, dslFactory).getIdForGlobalMetadata(dslFactory.create(component).get()).get();
    // MetadataCacheId declarationBasedId =
    // createDeclarationBasedGenerator(declaration).getIdForGlobalMetadata(elementDeclaration).get();

    assertThat(modelBasedId, equalTo(dslBasedId));

    // TODO: ADD THIS CHECK MULE-18685
    // assertThat(dslBasedId, equalTo(declarationBasedId));

    // TODO: ADD THIS CHECK MULE-18636
    // assertThat(declarationBasedId, astBasedId);

    // Any should be fine
    return modelBasedId;
  }

  private MetadataCacheIdGenerator<ComponentAst> createModelBasedGenerator(ArtifactAst app) {
    return new ModelBasedMetadataCacheIdGeneratorFactory()
        .create(dslResolvingContext, new ModelBasedTypeMetadataCacheKeyGeneratorTestCase.Locator(app));
  }

  private MetadataCacheIdGenerator<DslElementModel<?>> createDslBasedGenerator(ArtifactAst app,
                                                                               DslElementModelFactory factory) {
    ComponentLocator<ComponentAst> astLocator = new ModelBasedTypeMetadataCacheKeyGeneratorTestCase.Locator(app);
    ComponentLocator<DslElementModel<?>> dslLocator = l -> astLocator.get(l).map(e -> factory.create(e).orElse(null));
    return new DslElementBasedMetadataCacheIdGenerator(dslLocator);
  }

  private MetadataCacheIdGenerator<ElementDeclaration> createDeclarationBasedGenerator(ArtifactDeclaration app) {
    ComponentLocator<ElementDeclaration> declarationLocator =
        l -> app.findElement(builderFromStringRepresentation(l.toString()).build());
    return new DeclarationBasedMetadataCacheIdGenerator(dslResolvingContext, declarationLocator);
  }

  protected ArtifactAst loadApplicationModel(ArtifactDeclaration declaration) throws Exception {
    return toArtifactast(declaration, extensions);
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
