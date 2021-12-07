package org.mule.runtime.module.extension.internal.runtime.config;

import static org.mule.runtime.core.internal.component.ComponentAnnotations.ANNOTATION_COMPONENT_CONFIG;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.locator.ComponentLocator;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheIdGenerator;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheIdGeneratorFactory;
import org.mule.runtime.core.internal.registry.DefaultRegistry;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * ADD JDOC exclusive for ConfigurationProviderToolingAdapter
 *
 * @since 4.5
 */
public class ConfigurationCacheIdResolver {

  private Supplier<String> idSupplier;

  private LazyValue<MetadataCacheIdGenerator<ComponentAst>> cacheIdGeneratorLazyValue;

  public ConfigurationCacheIdResolver(MuleContext muleContext, ConfigurationProviderToolingAdapter configuration) {
    DefaultRegistry registry = new DefaultRegistry(muleContext);
    Optional<MetadataCacheIdGeneratorFactory> cacheIdGeneratorFactory =
      registry.<MetadataCacheIdGeneratorFactory>lookupByType(MetadataCacheIdGeneratorFactory.class);
    if(cacheIdGeneratorFactory.isPresent()) {
      cacheIdGeneratorLazyValue = new LazyValue(() -> {
        DslResolvingContext context = DslResolvingContext.getDefault(muleContext.getExtensionManager().getExtensions());
        ComponentLocator<ComponentAst> configLocator = location -> muleContext.getConfigurationComponentLocator()
          .find(location)
          .map(component -> (ComponentAst) component.getAnnotation(ANNOTATION_COMPONENT_CONFIG));

        return cacheIdGeneratorFactory.get().create(context, configLocator);
      });
    } else {
      setFallbackSupplier(configuration);
    }

  }

  private void setFallbackSupplier(ConfigurationProviderToolingAdapter configuration) {
    idSupplier = () -> configuration.getName();
  }

}
