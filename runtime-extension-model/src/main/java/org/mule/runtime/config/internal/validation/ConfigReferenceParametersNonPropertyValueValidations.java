/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.ast.api.validation.Validation.Level.WARN;

import static java.lang.String.format;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.graph.api.ArtifactAstDependencyGraphProvider;
import org.mule.runtime.ast.graph.api.ComponentAstDependency;

import java.util.function.Predicate;

public class ConfigReferenceParametersNonPropertyValueValidations extends AbstractReferenceParametersStereotypesValidations {

  public ConfigReferenceParametersNonPropertyValueValidations(ArtifactAstDependencyGraphProvider artifactAstDependencyGraphProvider) {
    super(artifactAstDependencyGraphProvider);
  }

  @Override
  public String getName() {
    return "Config Reference parameters point to fixed configs";
  }

  @Override
  public String getDescription() {
    return "Config Reference parameters point to fixed configs.";
  }

  @Override
  public Level getLevel() {
    return WARN;
  }

  @Override
  protected boolean filterComponent(ComponentAstDependency missingDependency) {
    return super.filterComponent(missingDependency)
        && missingDependency.getName().contains("${");
  }

  @Override
  protected Predicate<? super ComponentAstDependency> filterArtifact(ArtifactAst artifact) {
    return d -> true;
  }

  @Override
  protected String validationMessage(ComponentAstDependency missing) {

    return format("'%s:%s' has '%s' '%s' which is resolved with a property and may cause the artifact to have different behavior on different environments.",
                  missing.getComponent().getIdentifier().getNamespace(),
                  missing.getComponent().getIdentifier().getName(),
                  missing.getParameter().getModel().getName(),
                  missing.getName());
  }
}
