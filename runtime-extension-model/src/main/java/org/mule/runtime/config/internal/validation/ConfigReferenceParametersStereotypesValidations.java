/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.ast.api.ArtifactType.APPLICATION;
import static org.mule.runtime.ast.api.ArtifactType.DOMAIN;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.APP_CONFIG;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.CONFIG;
import static org.mule.runtime.internal.dsl.DslConstants.EE_PREFIX;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.graph.api.ComponentAstDependency;

import java.util.function.Predicate;

public class ConfigReferenceParametersStereotypesValidations extends AbstractReferenceParametersStereotypesValidations {

  public static final ComponentIdentifier CACHE_IDENTIFIER =
      builder().namespace(EE_PREFIX).name("cache").build();

  @Override
  public String getName() {
    return "Config Reference parameters stereotypes";
  }

  @Override
  public String getDescription() {
    return "Config Reference parameters point to declarations of the appropriate stereotype.";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  private Predicate<? super ComponentAstDependency> dependencyNotInDomainFilter(ArtifactAst artifact) {
    // Only domains expose their configs to apps
    if (artifact.getArtifactType().equals(APPLICATION)
        && artifact.getParent()
            .map(p -> p.getArtifactType().equals(DOMAIN))
            .orElse(false)) {

      return missing -> artifact.getParent().get().topLevelComponentsStream()
          .noneMatch(parentTopLevel -> parentTopLevel.getComponentId()
              .map(id -> id.equals(missing.getName()))
              .orElse(false));
    } else {
      return missing -> true;
    }
  }

  @Override
  protected Predicate<? super ComponentAstDependency> filter(ArtifactAst artifact) {
    Predicate<ComponentAstDependency> configPredicate = missing ->
    // Keep backwards compatibility with custom defined cachingStrategies
    !missing.getComponent().getIdentifier().equals(CACHE_IDENTIFIER)
        && missing.getAllowedStereotypes().stream()
            .anyMatch(st -> st.isAssignableTo(CONFIG)
                || st.isAssignableTo(APP_CONFIG));
    return configPredicate.and(dependencyNotInDomainFilter(artifact));
  }

}
