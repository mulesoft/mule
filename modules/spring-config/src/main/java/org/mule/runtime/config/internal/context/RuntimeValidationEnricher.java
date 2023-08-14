/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.context;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.ast.api.validation.ValidationsProvider;
import org.mule.runtime.ast.graph.api.ArtifactAstDependencyGraphProvider;
import org.mule.runtime.ast.graph.api.ArtifactAstGraphDependencyProviderAware;
import org.mule.runtime.core.api.MuleContext;

import java.util.function.Consumer;

/**
 * The runtime enricher for the {@link ValidationsProvider}'s.
 */
public class RuntimeValidationEnricher implements Consumer<ValidationsProvider> {

  private final ArtifactAstDependencyGraphProvider artifactAstGraphDependencyProvider;
  private final MuleContext muleContext;

  public RuntimeValidationEnricher(ArtifactAstDependencyGraphProvider artifactAstGraphDependencyProvider,
                                   MuleContext muleContext) {
    this.artifactAstGraphDependencyProvider = artifactAstGraphDependencyProvider;
    this.muleContext = muleContext;
  }

  @Override
  public void accept(ValidationsProvider validationsProvider) {
    injectDependencies(validationsProvider);
    setArtifactAstDependencyGraphProviderIfPossible(validationsProvider);
  }

  private void setArtifactAstDependencyGraphProviderIfPossible(ValidationsProvider validationsProvider) {
    if (validationsProvider instanceof ArtifactAstGraphDependencyProviderAware) {
      ((ArtifactAstGraphDependencyProviderAware) validationsProvider)
          .setArtifactAstDependencyGraphProvider(artifactAstGraphDependencyProvider);
    }
  }

  private void injectDependencies(ValidationsProvider validationsProvider) {
    try {
      muleContext.getInjector().inject(validationsProvider);
    } catch (MuleException e) {
      throw new MuleRuntimeException(e);
    }
  }
}
