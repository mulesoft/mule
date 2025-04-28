/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api;

import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.config.internal.model.ComponentBuildingDefinitionRegistryFactory.CACHING_FACTORY;
import static org.mule.runtime.config.internal.model.ComponentBuildingDefinitionRegistryFactory.DEFAULT_FACTORY;

import static java.lang.Boolean.getBoolean;

import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.config.internal.ArtifactAstConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link ConfigurationBuilder} specialization that can provide a new {@link ArtifactContextFactory}.
 *
 * @since 4.4
 */
public interface ArtifactContextFactory extends ConfigurationBuilder {

  static final String CACHE_COMPONENT_BUILDING_DEFINITION_REGISTRY_PROPERTY =
      SYSTEM_PROPERTY_PREFIX + ".functionalTest.cacheComponentBuildingDefinitionRegistry";
  /**
   * Disable the caching of the BuildingDefinitionRegistry, regardless of the value of
   * {@code mule.functionalTest.cacheComponentBuildingDefinitionRegistry}.
   */
  static final String CACHE_COMPONENT_BUILDING_DEFINITION_REGISTRY_DISABLE_OVERRIDE_PROPERTY =
      SYSTEM_PROPERTY_PREFIX + ".functionalTest.cacheComponentBuildingDefinitionRegistry.disableOverride";

  /**
   * Creates a new {@link ArtifactContextFactory} based on the given ArtifactctAst and its creation parameters.
   *
   * @since 4.7
   */
  public static ArtifactContextFactory createArtifactContextFactory(ArtifactAst artifactAst,
                                                                    Map<String, String> artifactProperties,
                                                                    ArtifactType artifactType,
                                                                    boolean enableLazyInit,
                                                                    boolean addToolingObjectsToRegistry,
                                                                    List<ServiceConfigurator> serviceConfigurators,
                                                                    Optional<ArtifactContext> parentArtifactContext)
      throws ConfigurationException {
    ArtifactAstConfigurationBuilder configurationBuilder =
        new ArtifactAstConfigurationBuilder(artifactAst,
                                            artifactProperties,
                                            artifactType,
                                            enableLazyInit,
                                            addToolingObjectsToRegistry,
                                            createComponentBuildingDefinitionRegistry(artifactAst));

    parentArtifactContext
        .ifPresent(parentContext -> configurationBuilder.setParentContext(parentContext.getMuleContext(),
                                                                          parentContext.getArtifactAst()));
    serviceConfigurators.stream().forEach(configurationBuilder::addServiceConfigurator);

    return configurationBuilder;
  }

  public static ComponentBuildingDefinitionRegistry createComponentBuildingDefinitionRegistry(ArtifactAst artifactAst) {
    if (getBoolean(CACHE_COMPONENT_BUILDING_DEFINITION_REGISTRY_PROPERTY)
        && !getBoolean(CACHE_COMPONENT_BUILDING_DEFINITION_REGISTRY_DISABLE_OVERRIDE_PROPERTY)) {
      return CACHING_FACTORY.create(artifactAst.dependencies(), artifactAst::dependenciesDsl);
    } else {
      return DEFAULT_FACTORY.create(artifactAst.dependencies(), artifactAst::dependenciesDsl);
    }
  }

  /**
   *
   * @return a new {@link ArtifactContext} from the state of this implementation.
   */
  ArtifactContext createArtifactContext();

}
