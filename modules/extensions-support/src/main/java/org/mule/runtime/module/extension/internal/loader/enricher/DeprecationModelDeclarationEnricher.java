/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.extension.api.loader.DeclarationEnricherPhase.POST_STRUCTURE;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConstructDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.FunctionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithDeprecatedDeclaration;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.extension.api.annotation.deprecated.Deprecated;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.loader.DeclarationEnricherPhase;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.model.deprecated.ImmutableDeprecationModel;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingParameterModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Enriches all the models that are annotatied with {@link Deprecated}. This is applicable to parameters, operations, sources,
 * functions, scopes, routers, configurations, connection providers and extensions.
 *
 * @since 4.2.0
 */
public class DeprecationModelDeclarationEnricher extends AbstractAnnotatedDeclarationEnricher {

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
    return POST_STRUCTURE;
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
        .ifPresent(deprecationAnnotation -> withDeprecatedDeclaration
            .withDeprecation(createDeprecationModel(deprecationAnnotation)));
  }

  private DeprecationModel createDeprecationModel(Deprecated deprecationAnnotation) {
    return new ImmutableDeprecationModel(deprecationAnnotation.message(), deprecationAnnotation.since(),
                                         isBlank(deprecationAnnotation.toRemoveIn()) ? null : deprecationAnnotation.toRemoveIn());
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
