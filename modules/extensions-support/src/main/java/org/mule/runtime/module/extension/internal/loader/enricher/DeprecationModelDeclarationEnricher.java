/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
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
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.DeclarationEnricherPhase;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.model.deprecated.ImmutableDeprecationModel;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingParameterModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
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

  private final Map<Class<? extends ModelProperty>, Function<ModelProperty, Optional<DeprecatedInformation>>> modelPropertiesClasses =
      createModelPropertiesClassesMap();

  private Map<Class<? extends ModelProperty>, Function<ModelProperty, Optional<DeprecatedInformation>>> createModelPropertiesClassesMap() {
    HashMap<Class<? extends ModelProperty>, Function<ModelProperty, Optional<DeprecatedInformation>>> modelPropertiesClassesMap =
        new HashMap<>();
    modelPropertiesClassesMap.put(ImplementingTypeModelProperty.class,
                                  (modelProperty -> getDeprecatedInformation((ImplementingTypeModelProperty) modelProperty)));
    modelPropertiesClassesMap.put(ImplementingParameterModelProperty.class,
                                  (modelProperty -> getDeprecatedInformation((ImplementingParameterModelProperty) modelProperty)));
    modelPropertiesClassesMap.put(ImplementingMethodModelProperty.class,
                                  (modelProperty -> getDeprecatedInformation((ImplementingMethodModelProperty) modelProperty)));
    return modelPropertiesClassesMap;
  }

  @Override
  public DeclarationEnricherPhase getExecutionPhase() {
    return POST_STRUCTURE;
  }

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {

    ExtensionDeclaration extensionDeclaration = extensionLoadingContext.getExtensionDeclarer().getDeclaration();

    enrichDeclaration(extensionDeclaration, ImplementingTypeModelProperty.class, extensionDeclaration.getName(), "extension");

    new IdempotentDeclarationWalker() {

      @Override
      protected void onConnectionProvider(ConnectionProviderDeclaration declaration) {
        enrichDeclaration(declaration, ImplementingTypeModelProperty.class, declaration.getName(), "connection provider");
      }

      @Override
      protected void onSource(SourceDeclaration declaration) {
        enrichDeclaration(declaration, ImplementingTypeModelProperty.class, declaration.getName(), "source");
      }

      @Override
      protected void onParameter(ParameterGroupDeclaration parameterGroup, ParameterDeclaration declaration) {
        enrichDeclaration(declaration, ImplementingParameterModelProperty.class, declaration.getName(), "parameter");
      }

      @Override
      protected void onConstruct(ConstructDeclaration declaration) {
        enrichDeclaration(declaration, ImplementingMethodModelProperty.class, declaration.getName(), "construct");
      }

      @Override
      protected void onConfiguration(ConfigurationDeclaration declaration) {
        enrichDeclaration(declaration, ImplementingTypeModelProperty.class, declaration.getName(), "configuration");
      }

      @Override
      protected void onOperation(OperationDeclaration declaration) {
        enrichDeclaration(declaration, ImplementingMethodModelProperty.class, declaration.getName(), "operation");
      }

      @Override
      protected void onFunction(FunctionDeclaration declaration) {
        enrichDeclaration(declaration, ImplementingMethodModelProperty.class, declaration.getName(), "function");
      }

    }.walk(extensionDeclaration);
  }

  private void enrichDeclaration(BaseDeclaration<?> declaration, Class<? extends ModelProperty> modelPropertyClass,
                                 String componentName, String componentType) {
    try {
      WithDeprecatedDeclaration withDeprecatedDeclaration;
      if (declaration instanceof WithDeprecatedDeclaration) {
        withDeprecatedDeclaration = (WithDeprecatedDeclaration) declaration;
        getDeprecatedInformation(declaration, modelPropertyClass)
            .ifPresent(deprecatedInformation -> withDeprecatedDeclaration
                .withDeprecation(createDeprecationModel(deprecatedInformation)));
      }
    } catch (IllegalArgumentException e) {
      throw new IllegalModelDefinitionException(format("Annotations %s and %s are both present at the same time on %s %s",
                                                       Deprecated.class.getName(),
                                                       org.mule.sdk.api.annotation.deprecated.Deprecated.class.getName(),
                                                       componentType, componentName));
    }
  }

  private DeprecationModel createDeprecationModel(DeprecatedInformation deprecatedInformation) {
    return new ImmutableDeprecationModel(deprecatedInformation.getMessage(), deprecatedInformation.getSince(),
                                         isBlank(deprecatedInformation.getToRemoveIn()) ? null
                                             : deprecatedInformation.getToRemoveIn());
  }

  private Optional<DeprecatedInformation> getDeprecatedInformation(BaseDeclaration<?> declaration,
                                                                   Class<? extends ModelProperty> modelPropertyClass) {
    Optional<? extends ModelProperty> modelProperty = declaration.getModelProperty(modelPropertyClass);
    if (modelProperty.isPresent()) {
      return modelPropertiesClasses.get(modelPropertyClass).apply(modelProperty.get());
    } else {
      return empty();
    }
  }

  private Optional<DeprecatedInformation> getDeprecatedInformation(ImplementingMethodModelProperty modelProperty) {
    Method method = modelProperty.getMethod();
    return getDeprecatedInformation(method.getAnnotation(Deprecated.class),
                                    method.getAnnotation(org.mule.sdk.api.annotation.deprecated.Deprecated.class));
  }

  private Optional<DeprecatedInformation> getDeprecatedInformation(ImplementingParameterModelProperty modelProperty) {
    Parameter parameter = modelProperty.getParameter();
    return getDeprecatedInformation(parameter.getAnnotation(Deprecated.class),
                                    parameter.getAnnotation(org.mule.sdk.api.annotation.deprecated.Deprecated.class));
  }

  private Optional<DeprecatedInformation> getDeprecatedInformation(ImplementingTypeModelProperty modelProperty) {
    Class<?> clazz = modelProperty.getType();
    return getDeprecatedInformation(clazz.getAnnotation(Deprecated.class),
                                    clazz.getAnnotation(org.mule.sdk.api.annotation.deprecated.Deprecated.class));
  }

  private Optional<DeprecatedInformation> getDeprecatedInformation(Deprecated legacyDeprecatedAnnotation,
                                                                   org.mule.sdk.api.annotation.deprecated.Deprecated sdkDeprecatedAnnotation)
      throws IllegalArgumentException {
    if (legacyDeprecatedAnnotation != null && sdkDeprecatedAnnotation != null) {
      throw new IllegalArgumentException();
    } else if (legacyDeprecatedAnnotation != null) {
      return of(new DeprecatedInformation(legacyDeprecatedAnnotation));
    } else if (sdkDeprecatedAnnotation != null) {
      return of(new DeprecatedInformation(sdkDeprecatedAnnotation));
    } else {
      return empty();
    }
  }

  private static class DeprecatedInformation {

    String message;
    String since;
    String toRemoveIn;

    public DeprecatedInformation(Deprecated deprecated) {
      this.message = deprecated.message();
      this.since = deprecated.since();
      this.toRemoveIn = deprecated.toRemoveIn();
    }

    public DeprecatedInformation(org.mule.sdk.api.annotation.deprecated.Deprecated deprecated) {
      this.message = deprecated.message();
      this.since = deprecated.since();
      this.toRemoveIn = deprecated.toRemoveIn();
    }

    public String getMessage() {
      return message;
    }

    public String getSince() {
      return since;
    }

    public String getToRemoveIn() {
      return toRemoveIn;
    }
  }

}
