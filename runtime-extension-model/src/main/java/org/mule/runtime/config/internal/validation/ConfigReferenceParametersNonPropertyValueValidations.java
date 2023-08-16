/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.ast.api.validation.Validation.Level.WARN;

import static java.lang.String.format;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.graph.api.ArtifactAstDependencyGraphProvider;
import org.mule.runtime.ast.graph.api.ComponentAstDependency;

import java.util.function.Predicate;

public class ConfigReferenceParametersNonPropertyValueValidations extends AbstractReferenceParametersStereotypesValidations {

  private final boolean enabled;

  public ConfigReferenceParametersNonPropertyValueValidations(boolean enabled,
                                                              ArtifactAstDependencyGraphProvider artifactAstDependencyGraphProvider) {
    super(artifactAstDependencyGraphProvider);
    this.enabled = enabled;
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
        && enabled
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
