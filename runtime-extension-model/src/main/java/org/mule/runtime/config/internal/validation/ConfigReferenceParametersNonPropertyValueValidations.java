/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.ast.api.validation.Validation.Level.WARN;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.graph.api.ComponentAstDependency;

import java.util.function.Predicate;

public class ConfigReferenceParametersNonPropertyValueValidations extends AbstractReferenceParametersStereotypesValidations {

  private final boolean enabled;

  public ConfigReferenceParametersNonPropertyValueValidations(boolean enabled) {
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

}
