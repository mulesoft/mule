/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.declaration.fluent.*;
import org.mule.runtime.extension.api.annotation.deprecated.Deprecated;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.loader.DeclarationEnricherPhase;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.model.deprecated.ImmutableDeprecatedModel;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingParameterModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;

import java.util.*;
import java.util.function.Function;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

/**
 * ADD JAVA DOC
 *
 * @since 4.2.0
 */
public class DeprecatedModelDeclarationEnricher extends AbstractAnnotatedDeclarationEnricher {

  private final Map<Class<? extends ModelProperty>, Function<ModelProperty, Optional<Deprecated>>> modelPropertiesClasses =
      createModelPropertiesClassesMap();

  private Map<Class<? extends ModelProperty>, Function<ModelProperty, Optional<Deprecated>>> createModelPropertiesClassesMap() {
    HashMap<Class<? extends ModelProperty>, Function<ModelProperty, Optional<Deprecated>>> modelPropertiesClassesMap =
        new HashMap<>();
    modelPropertiesClassesMap.put(ImplementingTypeModelProperty.class,
                                  (modelProperty -> getDeprecatedAnnotation((ImplementingTypeModelProperty) modelProperty)));
    modelPropertiesClassesMap.put(ImplementingParameterModelProperty.class,
                                  (modelProperty -> getDeprecatedAnnotation((ImplementingParameterModelProperty) modelProperty)));
    modelPropertiesClassesMap.put(ImplementingMethodModelProperty.class,
                                  (modelProperty -> getDeprecatedAnnotation((ImplementingMethodModelProperty) modelProperty)));
    return modelPropertiesClassesMap;
  }

  @Override
  public DeclarationEnricherPhase getExecutionPhase() {
    return DeclarationEnricherPhase.POST_STRUCTURE;
  }

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {

    ExtensionDeclaration extensionDeclaration = extensionLoadingContext.getExtensionDeclarer().getDeclaration();

    enrichDeclaration(extensionDeclaration, ImplementingTypeModelProperty.class);

    new IdempotentDeclarationWalker() {

      @Override
      protected void onConnectionProvider(ConnectionProviderDeclaration declaration) {
        enrichDeclaration(declaration, ImplementingTypeModelProperty.class);
      }

      @Override
      protected void onSource(SourceDeclaration declaration) {
        enrichDeclaration(declaration, ImplementingTypeModelProperty.class);
      }

      @Override
      protected void onParameter(ParameterGroupDeclaration parameterGroup, ParameterDeclaration declaration) {
        enrichDeclaration(declaration, ImplementingParameterModelProperty.class);
      }

      @Override
      protected void onConstruct(ConstructDeclaration declaration) {
        enrichDeclaration(declaration, ImplementingMethodModelProperty.class);
      }

      @Override
      protected void onConfiguration(ConfigurationDeclaration declaration) {
        enrichDeclaration(declaration, ImplementingTypeModelProperty.class);
      }

      @Override
      protected void onOperation(OperationDeclaration declaration) {
        enrichDeclaration(declaration, ImplementingMethodModelProperty.class);
      }

      @Override
      protected void onFunction(FunctionDeclaration declaration) {
        enrichDeclaration(declaration, ImplementingMethodModelProperty.class);
      }

    }.walk(extensionDeclaration);
  }

  private void enrichDeclaration(BaseDeclaration<?> declaration, Class<? extends ModelProperty> modelPropertyClass) {
    WithDeprecatedDeclaration withDeprecatedDeclaration;
    if (declaration instanceof WithDeprecatedDeclaration) {
      withDeprecatedDeclaration = (WithDeprecatedDeclaration) declaration;
    } else {
      return;
    }

    getDeprecatedAnnotation(declaration, modelPropertyClass)
        .ifPresent(deprecated -> withDeprecatedDeclaration.withDeprecation(new ImmutableDeprecatedModel(deprecated.message())));
  }

  private Optional<Deprecated> getDeprecatedAnnotation(BaseDeclaration<?> declaration,
                                                       Class<? extends ModelProperty> modelPropertyClass) {
    Optional<? extends ModelProperty> modelProperty = declaration.getModelProperty(modelPropertyClass);
    if (modelProperty.isPresent()) {
      return modelPropertiesClasses.get(modelPropertyClass).apply(modelProperty.get());
    } else {
      return empty();
    }
  }

  private Optional<Deprecated> getDeprecatedAnnotation(ImplementingMethodModelProperty modelProperty) {
    return ofNullable(modelProperty.getMethod().getAnnotation(Deprecated.class));
  }

  private Optional<Deprecated> getDeprecatedAnnotation(ImplementingParameterModelProperty modelProperty) {
    return ofNullable(modelProperty.getParameter().getAnnotation(Deprecated.class));
  }

  private Optional<Deprecated> getDeprecatedAnnotation(ImplementingTypeModelProperty modelProperty) {
    return ofNullable(modelProperty.getType().getAnnotation(Deprecated.class));
  }

}
