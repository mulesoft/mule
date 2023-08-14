/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.classloading;

import static java.util.Collections.singletonList;

import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationsProvider;
import org.mule.runtime.ast.graph.api.ArtifactAstDependencyGraphProvider;
import org.mule.runtime.ast.graph.api.ArtifactAstGraphDependencyProviderAware;

import java.util.List;

public class ArtifactAstDependencyGraphProviderValidationsProvider
    implements ValidationsProvider, ArtifactAstGraphDependencyProviderAware {

  private ArtifactAstDependencyGraphProvider artifactAstDependencyGraphProvider;

  @Override
  public List<Validation> get() {
    return singletonList(new ArtifactAstDependencyGraphProviderValidation(artifactAstDependencyGraphProvider));
  }

  @Override
  public void setArtifactAstDependencyGraphProvider(ArtifactAstDependencyGraphProvider artifactAstDependencyGraphProvider) {
    this.artifactAstDependencyGraphProvider = artifactAstDependencyGraphProvider;
  }
}
