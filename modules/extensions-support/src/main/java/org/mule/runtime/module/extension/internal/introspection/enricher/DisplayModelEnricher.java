/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotation;

import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.fluent.BaseDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ConnectedDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.OperationDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ParameterizedInterceptableDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.SourceDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.WithOperationsDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.WithSourcesDeclaration;
import org.mule.runtime.extension.api.introspection.property.DisplayModelProperty;
import org.mule.runtime.module.extension.internal.model.property.DeclaringMemberModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingParameterModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.util.IdempotentDeclarationWalker;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

/**
 * Enriches the {@link ExtensionDeclarer} with a {@link DisplayModelProperty} from annotated elements with {@link Summary} or
 * {@link DisplayName}
 *
 * @since 4.0
 */
public final class DisplayModelEnricher extends AbstractAnnotatedModelEnricher {

  @Override
  public void enrich(DescribingContext describingContext) {
    new IdempotentDeclarationWalker() {

      @Override
      public void onSource(WithSourcesDeclaration owner, SourceDeclaration declaration) {
        enrichTypes(declaration);
      }

      @Override
      public void onParameter(ParameterizedInterceptableDeclaration owner, ParameterDeclaration declaration) {
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
    }.walk(describingContext.getExtensionDeclarer().getDeclaration());
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

      createDisplayModelProperty(declaration, summaryAnnotation, displayNameAnnotation);
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

      createDisplayModelProperty(declaration, summaryAnnotation, displayNameAnnotation);
    }
  }

  private void createDisplayModelProperty(BaseDeclaration declaration, Summary summaryAnnotation,
                                          DisplayName displayNameAnnotation) {
    String summary = summaryAnnotation != null ? summaryAnnotation.value() : null;
    String displayName = displayNameAnnotation != null ? displayNameAnnotation.value() : null;

    if (summary != null || displayName != null) {
      declaration.addModelProperty(new DisplayModelProperty(displayName, summary));
    }
  }
}
