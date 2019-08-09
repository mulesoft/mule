/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static org.mule.runtime.extension.api.annotation.source.SourceClusterSupport.DEFAULT_PRIMARY_NODE_ONLY;
import static org.mule.runtime.extension.api.annotation.source.SourceClusterSupport.NOT_SUPPORTED;
import static org.mule.runtime.extension.api.loader.DeclarationEnricherPhase.WIRING;
import static org.mule.runtime.extension.internal.loader.util.InfrastructureParameterBuilder.addPrimaryNodeParameter;

import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.extension.api.ExtensionConstants;
import org.mule.runtime.extension.api.annotation.source.ClusterSupport;
import org.mule.runtime.extension.api.annotation.source.SourceClusterSupport;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.loader.DeclarationEnricherPhase;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;

import java.lang.annotation.Annotation;
import java.util.Optional;

/**
 * Adds a #{@link ExtensionConstants#PRIMARY_NODE_ONLY_PARAMETER_NAME} parameter on all sources for which
 * {@link SourceModel#runsOnPrimaryNodeOnly()} is {@code false}
 *
 * @since 4.1
 */
public class ClusterSupportEnricher extends AbstractAnnotatedDeclarationEnricher {

  @Override
  public DeclarationEnricherPhase getExecutionPhase() {
    return WIRING;
  }

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    new IdempotentDeclarationWalker() {

      @Override
      protected void onSource(SourceDeclaration declaration) {
        ClusterSupport annotation = getAnnotation(declaration, ClusterSupport.class).orElse(null);

        if (annotation != null) {
          SourceClusterSupport clusterSupport = annotation.value();
          if (clusterSupport == NOT_SUPPORTED) {
            declaration.setRunsOnPrimaryNodeOnly(true);
          } else {
            declaration.setRunsOnPrimaryNodeOnly(false);
            addPrimaryNodeParameter(declaration, clusterSupport == DEFAULT_PRIMARY_NODE_ONLY);
          }
        } else {
          declaration.setRunsOnPrimaryNodeOnly(false);
          addPrimaryNodeParameter(declaration, false);
        }
      }
    }.walk(extensionLoadingContext.getExtensionDeclarer().getDeclaration());
  }

  private <T extends Annotation> Optional<T> getAnnotation(SourceDeclaration declaration, Class<T> annotation) {
    return declaration.getModelProperty(ExtensionTypeDescriptorModelProperty.class)
        .map(ExtensionTypeDescriptorModelProperty::getType)
        .filter(type -> type.isAnnotatedWith(annotation))
        .flatMap(type -> type.getAnnotation(annotation));
  }

}
