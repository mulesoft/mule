/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static java.lang.String.format;
import static org.mule.runtime.core.internal.component.ComponentAnnotations.ANNOTATION_COMPONENT_CONFIG;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.locator.ComponentLocator;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheIdGenerator;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheIdGeneratorFactory;
import org.mule.runtime.core.internal.registry.DefaultRegistry;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Class that resolves the id used to retrieve the cache for metadata resolutions for {@link ConfigurationProviderToolingAdapter}
 *
 * @since 4.5
 */
public class ConfigurationCacheIdResolver {

  private Supplier<String> idSupplier;

  public ConfigurationCacheIdResolver(MuleContext muleContext, ConfigurationProvider configuration) {
    generateConfigurationCacheIdSupplier(muleContext, configuration);
  }

  public String getConfigurationCacheId() {
    return idSupplier.get();
  }

  private void generateConfigurationCacheIdSupplier(MuleContext muleContext, ConfigurationProvider configuration) {
    DefaultRegistry registry = new DefaultRegistry(muleContext);
    Optional<MetadataCacheIdGeneratorFactory> cacheIdGeneratorFactory =
        registry.lookupByType(MetadataCacheIdGeneratorFactory.class);
    if (cacheIdGeneratorFactory.isPresent()) {
      idSupplier = new LazyValue(() -> {
        try {
          DslResolvingContext context = DslResolvingContext.getDefault(muleContext.getExtensionManager().getExtensions());
          ComponentLocator<ComponentAst> configLocator = location -> muleContext.getConfigurationComponentLocator()
              .find(location)
              .map(component -> (ComponentAst) component.getAnnotation(ANNOTATION_COMPONENT_CONFIG));

          MetadataCacheIdGenerator<ComponentAst> cacheIdGenerator =
              cacheIdGeneratorFactory.get().create(context, configLocator);
          return cacheIdGenerator
              .getIdForGlobalMetadata((ComponentAst) configuration.getAnnotation(ANNOTATION_COMPONENT_CONFIG))
              .orElseThrow(() -> new IllegalStateException(
                                                           format("Missing information to obtain the MetadataCache for the component '%s'. "
                                                               +
                                                               "Expected to have the ComponentAst information in the '%s' annotation but none was found.",
                                                                  configuration.getLocation().toString(),
                                                                  ANNOTATION_COMPONENT_CONFIG)))
              .getValue();

        } catch (Exception e) {
          return configuration.getName();
        }
      });
    } else {
      idSupplier = new LazyValue<>(() -> configuration.getName());
    }
  }

}
