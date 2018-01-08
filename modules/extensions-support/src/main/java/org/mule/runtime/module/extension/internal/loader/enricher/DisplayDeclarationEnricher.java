/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static org.mule.runtime.extension.api.loader.DeclarationEnricherPhase.LAYOUT;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.toClassValueModel;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotation;

import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithOperationsDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithSourcesDeclaration;
import org.mule.runtime.api.meta.model.display.ClassValueModel;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.PathModel;
import org.mule.runtime.extension.api.annotation.param.display.ClassValue;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Path;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.loader.DeclarationEnricherPhase;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.WithAnnotations;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionOperationDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionParameterDescriptorModelProperty;

import java.util.Optional;

/**
 * Enriches the {@link ExtensionDeclarer} with a {@link DisplayModel} from annotated elements with {@link Summary} or
 * {@link DisplayName}
 *
 * @since 4.0
 */
public final class DisplayDeclarationEnricher extends AbstractAnnotatedDeclarationEnricher {


  @Override
  public DeclarationEnricherPhase getExecutionPhase() {
    return LAYOUT;
  }

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    new IdempotentDeclarationWalker() {

      @Override
      public void onSource(WithSourcesDeclaration owner, SourceDeclaration declaration) {
        enrichTypes(declaration);
      }

      @Override
      protected void onParameter(ParameterGroupDeclaration parameterGroup, ParameterDeclaration declaration) {
        enrichParameter(declaration);
      }

      @Override
      public void onOperation(WithOperationsDeclaration owner, OperationDeclaration declaration) {
        enrichOperation(declaration);
      }

      @Override
      public void onConnectionProvider(ConnectedDeclaration owner, ConnectionProviderDeclaration declaration) {
        enrichTypes(declaration);
      }

      @Override
      public void onConfiguration(ConfigurationDeclaration declaration) {
        enrichTypes(declaration);
      }
    }.walk(extensionLoadingContext.getExtensionDeclarer().getDeclaration());
  }

  private void enrichParameter(ParameterDeclaration declaration) {
    declaration.getModelProperty(ExtensionParameterDescriptorModelProperty.class)
        .ifPresent(modelProperty -> enrichDeclaration(declaration, modelProperty.getExtensionParameter()));

  }

  private void enrichTypes(BaseDeclaration declaration) {
    final Optional<ImplementingTypeModelProperty> modelProperty =
        declaration.getModelProperty(ImplementingTypeModelProperty.class);
    if (modelProperty.isPresent()) {
      final Class<?> annotatedType = modelProperty.get().getType();
      final Summary summaryAnnotation = getAnnotation(annotatedType, Summary.class);
      final DisplayName displayNameAnnotation = getAnnotation(annotatedType, DisplayName.class);
      final Example exampleAnnotation = getAnnotation(annotatedType, Example.class);
      final Path pathAnnotation = getAnnotation(annotatedType, Path.class);
      final ClassValue classAnnotation = getAnnotation(annotatedType, ClassValue.class);

      createDisplayModelProperty(declaration, summaryAnnotation, displayNameAnnotation, exampleAnnotation, pathAnnotation,
                                 classAnnotation);
    }
  }

  private void enrichOperation(OperationDeclaration declaration) {
    declaration.getModelProperty(ExtensionOperationDescriptorModelProperty.class)
        .map(ExtensionOperationDescriptorModelProperty::getOperationMethod)
        .ifPresent(annotatedElement -> enrichDeclaration(declaration, annotatedElement));
  }

  private void enrichDeclaration(BaseDeclaration declaration, WithAnnotations annotatedElement) {
    if (annotatedElement != null) {
      final Summary summaryAnnotation = annotatedElement.getAnnotation(Summary.class).orElse(null);
      final DisplayName displayNameAnnotation = annotatedElement.getAnnotation(DisplayName.class).orElse(null);
      final Example exampleAnnotation = annotatedElement.getAnnotation(Example.class).orElse(null);
      final Path pathAnnotation = annotatedElement.getAnnotation(Path.class).orElse(null);
      final ClassValue classValue = annotatedElement.getAnnotation(ClassValue.class).orElse(null);

      createDisplayModelProperty(declaration, summaryAnnotation, displayNameAnnotation, exampleAnnotation, pathAnnotation,
                                 classValue);
    }
  }

  private void createDisplayModelProperty(BaseDeclaration declaration,
                                          Summary summaryAnnotation,
                                          DisplayName displayNameAnnotation,
                                          Example exampleAnnotation,
                                          Path pathAnnotation,
                                          ClassValue classValue) {
    String summary = summaryAnnotation != null ? summaryAnnotation.value() : null;
    String displayName = displayNameAnnotation != null ? displayNameAnnotation.value() : null;
    String example = exampleAnnotation != null ? exampleAnnotation.value() : null;
    PathModel pathModel = null;
    ClassValueModel classValueModel = null;

    if (pathAnnotation != null) {
      pathModel = new PathModel(pathAnnotation.type(),
                                pathAnnotation.acceptsUrls(),
                                pathAnnotation.location(),
                                pathAnnotation.acceptedFileExtensions());
    }

    if (classValue != null) {
      classValueModel = toClassValueModel(classValue);
    }

    if (summary != null || displayName != null || example != null || pathModel != null || classValueModel != null) {
      declaration.setDisplayModel(DisplayModel.builder()
          .displayName(displayName)
          .summary(summary)
          .example(example)
          .path(pathModel)
          .classValue(classValueModel)
          .build());
    }
  }
}
