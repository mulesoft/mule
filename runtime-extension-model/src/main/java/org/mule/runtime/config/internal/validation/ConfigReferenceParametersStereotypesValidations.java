/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.config.MuleRuntimeFeature.ENABLE_POLICY_ISOLATION;
import static org.mule.runtime.ast.api.ArtifactType.APPLICATION;
import static org.mule.runtime.ast.api.ArtifactType.DOMAIN;
import static org.mule.runtime.ast.api.ArtifactType.POLICY;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.internal.dsl.DslConstants.EE_PREFIX;
import static org.mule.sdk.api.stereotype.MuleStereotypes.APP_CONFIG;
import static org.mule.sdk.api.stereotype.MuleStereotypes.CONFIG;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.graph.api.ComponentAstDependency;

import java.util.Optional;
import java.util.function.Predicate;

public class ConfigReferenceParametersStereotypesValidations extends AbstractReferenceParametersStereotypesValidations {

  private final Optional<FeatureFlaggingService> featureFlaggingService;
  private final boolean waiveUnresolvedPropertiesOnParams;

  public ConfigReferenceParametersStereotypesValidations(Optional<FeatureFlaggingService> featureFlaggingService,
                                                         boolean waiveUnresolvedPropertiesOnParams) {
    this.featureFlaggingService = featureFlaggingService;
    this.waiveUnresolvedPropertiesOnParams = waiveUnresolvedPropertiesOnParams;
  }

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
    if (domainFromApp(artifact)) {
      return missing -> artifact.getParent().get().topLevelComponentsStream()
          .noneMatch(parentTopLevel -> parentTopLevel.getComponentId()
              .map(id -> id.equals(missing.getName()))
              .orElse(false));
    } else {
      return missing -> true;
    }
  }

  protected boolean domainFromApp(ArtifactAst artifact) {
    // Only domains expose their configs to apps...
    return artifact.getArtifactType().equals(APPLICATION)
        && artifact.getParent()
            .map(p -> p.getArtifactType().equals(DOMAIN))
            .orElse(false);
  }

  private Predicate<? super ComponentAstDependency> dependencyNotInAppFilter(ArtifactAst artifact) {
    if (appFromPolicy(artifact)) {
      return missing -> artifact.getParent().get().topLevelComponentsStream()
          .noneMatch(parentTopLevel -> parentTopLevel.getComponentId()
              .map(id -> id.equals(missing.getName()))
              .orElse(false));
    } else {
      return missing -> true;
    }
  }

  protected boolean appFromPolicy(ArtifactAst artifact) {
    // ... or apps to policies if explicitely enabled
    return artifact.getArtifactType().equals(POLICY)
        && featureFlaggingService.map(ffs -> !ffs.isEnabled(ENABLE_POLICY_ISOLATION)).orElse(false)
        && artifact.getParent()
            .map(p -> p.getArtifactType().equals(APPLICATION))
            .orElse(false);
  }

  @Override
  protected boolean filterComponent(ComponentAstDependency missingDependency) {
    return super.filterComponent(missingDependency)
        && !(waiveUnresolvedPropertiesOnParams
            || missingDependency.getName().contains("${"));
  }

  @Override
  protected Predicate<? super ComponentAstDependency> filterArtifact(ArtifactAst artifact) {
    Predicate<ComponentAstDependency> configPredicate = missing ->
    // Keep backwards compatibility with custom defined cachingStrategies
    !missing.getComponent().getIdentifier().equals(CACHE_IDENTIFIER)
        && missing.getAllowedStereotypes().stream()
            .anyMatch(st -> st.isAssignableTo(CONFIG)
                || st.isAssignableTo(APP_CONFIG));

    return configPredicate
        .and(dependencyNotInDomainFilter(artifact))
        .and(dependencyNotInAppFilter(artifact));
  }

}
