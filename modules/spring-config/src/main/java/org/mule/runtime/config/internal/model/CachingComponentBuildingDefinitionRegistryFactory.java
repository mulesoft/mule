/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model;

import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_DYNAMIC_CONFIG_REF_PROPERTY;
import static org.mule.runtime.extension.api.ocs.OCSConstants.OCS_ENABLED;

import static java.lang.Boolean.getBoolean;
import static java.util.Optional.ofNullable;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toMap;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class CachingComponentBuildingDefinitionRegistryFactory implements ComponentBuildingDefinitionRegistryFactory {

  private final Map<ComponentBuildingDefinitionRegistryCacheKey, ComponentBuildingDefinitionRegistry> componentBuildingDefinitionRegistryCache =
      new HashMap<>();

  @Override
  public ComponentBuildingDefinitionRegistry create(Set<ExtensionModel> extensionModels,
                                                    Function<ExtensionModel, Optional<DslSyntaxResolver>> dslSyntaxResolverLookup) {
    return componentBuildingDefinitionRegistryCache
        .computeIfAbsent(new ComponentBuildingDefinitionRegistryCacheKey(extensionModels,
                                                                         getBoolean(ENABLE_DYNAMIC_CONFIG_REF_PROPERTY),
                                                                         getBoolean(OCS_ENABLED)),
                         key -> createComponentBuildingDefinitionRegistry(key.getExtensions(), dslSyntaxResolverLookup));
  }

  // Avoid the dslSyntaxResolverLookup form keeping a reference to the first artifactAst
  public ComponentBuildingDefinitionRegistry createComponentBuildingDefinitionRegistry(Set<ExtensionModel> extModels,
                                                                                       Function<ExtensionModel, Optional<DslSyntaxResolver>> dslSyntaxResolverLookup) {
    final Map<ExtensionModel, DslSyntaxResolver> extModelsDsls = extModels
        .stream()
        .filter(ext -> dslSyntaxResolverLookup.apply(ext).isPresent())
        .collect(toMap(identity(), ext -> dslSyntaxResolverLookup.apply(ext).orElseThrow()));

    return DEFAULT_FACTORY.create(extModels, extModel -> ofNullable(extModelsDsls.get(extModel)));
  }

  private class ComponentBuildingDefinitionRegistryCacheKey {

    private Set<ExtensionModel> extensions;
    private boolean dynamiConfigsEnabled;
    private boolean ocsEnabled;

    public ComponentBuildingDefinitionRegistryCacheKey(Set<ExtensionModel> extensions, boolean dynamiConfigsEnabled,
                                                       boolean ocsEnabled) {
      this.extensions = extensions;
      this.dynamiConfigsEnabled = dynamiConfigsEnabled;
    }

    public Set<ExtensionModel> getExtensions() {
      return extensions;
    }

    public boolean isDynamiConfigsEnabled() {
      return dynamiConfigsEnabled;
    }

    public boolean isOcsEnabled() {
      return ocsEnabled;
    }

    @Override
    public int hashCode() {
      return Objects.hash(dynamiConfigsEnabled, extensions, ocsEnabled);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      ComponentBuildingDefinitionRegistryCacheKey other = (ComponentBuildingDefinitionRegistryCacheKey) obj;
      return dynamiConfigsEnabled == other.dynamiConfigsEnabled
          && ocsEnabled == other.ocsEnabled
          && Objects.equals(extensions, other.extensions);
    }

  }

}
