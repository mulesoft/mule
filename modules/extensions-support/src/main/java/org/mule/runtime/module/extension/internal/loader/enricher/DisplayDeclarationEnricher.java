/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

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
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.PathModel;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Path;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.property.DeclaringMemberModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingParameterModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

/**
 * Enriches the {@link ExtensionDeclarer} with a {@link DisplayModel} from annotated elements with {@link Summary} or
 * {@link DisplayName}
 *
 * @since 4.0
 */
public final class DisplayDeclarationEnricher extends AbstractAnnotatedDeclarationEnricher {

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
    final Optional<DeclaringMemberModelProperty> declaringMemberProperty =
        declaration.getModelProperty(DeclaringMemberModelProperty.class);
    final Optional<ImplementingParameterModelProperty> implementingParameterProperty =
        declaration.getModelProperty(ImplementingParameterModelProperty.class);
    AnnotatedElement annotatedElement = null;

    if (declaringMemberProperty.isPresent()) {
      annotatedElement = declaringMemberProperty.get().getDeclaringField();
    }

    if (implementingParameterProperty.isPresent()) {
      annotatedElement = implementingParameterProperty.get().getParameter();
    }

    enrichDeclaration(declaration, annotatedElement);
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
      createDisplayModelProperty(declaration, summaryAnnotation, displayNameAnnotation, exampleAnnotation, pathAnnotation);
    }
  }

  private void enrichOperation(OperationDeclaration declaration) {
    final Optional<ImplementingMethodModelProperty> modelProperty =
        declaration.getModelProperty(ImplementingMethodModelProperty.class);
    AnnotatedElement annotatedElement = null;

    if (modelProperty.isPresent()) {
      annotatedElement = modelProperty.get().getMethod();
    }
    enrichDeclaration(declaration, annotatedElement);
  }

  private void enrichDeclaration(BaseDeclaration declaration, AnnotatedElement annotatedElement) {
    if (annotatedElement != null) {
      final Summary summaryAnnotation = annotatedElement.getAnnotation(Summary.class);
      final DisplayName displayNameAnnotation = annotatedElement.getAnnotation(DisplayName.class);
      final Example exampleAnnotation = annotatedElement.getAnnotation(Example.class);
      final Path pathAnnotation = annotatedElement.getAnnotation(Path.class);
      createDisplayModelProperty(declaration, summaryAnnotation, displayNameAnnotation, exampleAnnotation, pathAnnotation);
    }
  }

  private void createDisplayModelProperty(BaseDeclaration declaration,
                                          Summary summaryAnnotation,
                                          DisplayName displayNameAnnotation,
                                          Example exampleAnnotation,
                                          Path pathAnnotation) {
    String summary = summaryAnnotation != null ? summaryAnnotation.value() : null;
    String displayName = displayNameAnnotation != null ? displayNameAnnotation.value() : null;
    String example = exampleAnnotation != null ? exampleAnnotation.value() : null;
    PathModel pathModel = null;
    if (pathAnnotation != null) {
      pathModel = new PathModel(pathAnnotation.type(),
                                pathAnnotation.acceptsUrls(),
                                pathAnnotation.location(),
                                pathAnnotation.acceptedFileExtensions());
    }

    if (summary != null || displayName != null || example != null || pathModel != null) {
      declaration.setDisplayModel(DisplayModel.builder()
          .displayName(displayName)
          .summary(summary)
          .example(example)
          .path(pathModel)
          .build());
    }
  }
}
