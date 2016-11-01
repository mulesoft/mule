/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getParameterClasses;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.transformer.simple.StringToEnum;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.ExpirableConfigurationProvider;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hold the state related to registered {@link ExtensionModel extensionModels} and their instances.
 * <p>
 * It also provides utility methods and caches to easily locate pieces of such state.
 * <p>
 * It acts as a facade of the {@link MuleRegistry}, which is where {@link ConfigurationProvider} are finally stored.
 *
 * @since 3.7.0
 */
final class ExtensionRegistry {

  private final LoadingCache<ExtensionModel, List<ConfigurationProvider>> providersByExtension =
      CacheBuilder.newBuilder().build(new CacheLoader<ExtensionModel, List<ConfigurationProvider>>() {

        @Override
        public List<ConfigurationProvider> load(ExtensionModel key) throws Exception {
          return registry.lookupObjects(ConfigurationProvider.class).stream()
              .filter(provider -> provider.getExtensionModel() == key).collect(new ImmutableListCollector<>());
        }
      });

  private final Map<ExtensionEntityKey, ExtensionModel> extensions = new ConcurrentHashMap<>();
  private final Set<Class<? extends Enum>> enumClasses = new HashSet<>();
  private final MuleRegistry registry;

  /**
   * Creates a new instance
   *
   * @param registry the {@link MuleRegistry} to use for holding instances
   */
  ExtensionRegistry(MuleRegistry registry) {
    this.registry = registry;
  }

  /**
   * Registers the given {@code extension}
   *
   * @param name the registration name you want for the {@code extension}
   * @param extensionModel a {@link ExtensionModel}
   */
  void registerExtension(String name, ExtensionModel extensionModel) {
    extensions.put(new ExtensionEntityKey(name), extensionModel);
    getParameterClasses(extensionModel, Thread.currentThread().getContextClassLoader()).stream()
        .filter(type -> Enum.class.isAssignableFrom(type))
        .forEach(type -> {
          final Class<Enum> enumClass = (Class<Enum>) type;
          if (enumClasses.add(enumClass)) {
            try {
              registry.registerTransformer(new StringToEnum(enumClass));
            } catch (MuleException e) {
              throw new MuleRuntimeException(createStaticMessage("Could not register transformer for enum "
                  + enumClass.getName()), e);
            }
          }
        });
  }

  /**
   * @return an immutable view of the currently registered {@link ExtensionModel}
   */
  Set<ExtensionModel> getExtensions() {
    return ImmutableSet.copyOf(extensions.values());
  }

  /**
   * @return an {@link Optional} with the {@link ExtensionModel} which name and vendor equals {@code extensionName} and
   *         {@code vendor}
   */
  Optional<ExtensionModel> getExtension(String extensionName) {
    return Optional.ofNullable(extensions.get(new ExtensionEntityKey(extensionName)));
  }

  /**
   * @param name the registration name of the {@link ExtensionModel} you want to test
   * @return {@code true} if an {@link ExtensionModel} is registered under {@code name}. {@code false} otherwise
   */
  boolean containsExtension(String name) {
    return extensions.containsKey(new ExtensionEntityKey(name));

  }

  /**
   * Returns all the {@link ConfigurationProvider configuration providers} which serve {@link ConfigurationModel configuration
   * models} owned by {@code extensionModel}
   *
   * @param extensionModel a registered {@link ExtensionModel}
   * @return an immutable {@link List}. Might be empty but will never be {@code null}
   */
  List<ConfigurationProvider> getConfigurationProviders(ExtensionModel extensionModel) {
    return providersByExtension.getUnchecked(extensionModel);
  }

  /**
   * Returns the {@link ConfigurationProvider} registered under the given {@code key}
   *
   * @param key the key for the fetched {@link ConfigurationProvider}
   * @return a {@link ConfigurationProvider}
   */
  Optional<ConfigurationProvider> getConfigurationProvider(String key) {
    return Optional.ofNullable(registry.get(key));
  }

  /**
   * Registers the given {@code configurationProvider} in the underlying {@link #registry}.
   * <p>
   * The {@code configurationProvider} is registered under a key matching its {@link ConfigurationProvider#getName()}.
   *
   * @param configurationProvider a {@link ConfigurationProvider} to be registered
   * @throws IllegalArgumentException if {@code configurationProvider} is {@code null}
   * @throws MuleRuntimeException if the {@code configurationProvider} could not be registered
   */
  void registerConfigurationProvider(ConfigurationProvider configurationProvider) {
    checkArgument(configurationProvider != null, "Cannot register a null configurationProvider");
    try {
      registry.registerObject(configurationProvider.getName(), configurationProvider);
    } catch (RegistrationException e) {
      throw new MuleRuntimeException(createStaticMessage(format("Found exception while registering configuration provider '%s'",
                                                                configurationProvider.getName())),
                                     e);
    }

    providersByExtension.invalidate(configurationProvider.getExtensionModel());
  }

  /**
   * Returns a {@link Multimap} which keys are registrations keys and the values are the {@link ConfigurationInstance} instances
   * which are expired
   *
   * @return an immutable {@link Multimap}
   */
  Multimap<String, ConfigurationInstance> getExpiredConfigs() {
    ListMultimap<String, ConfigurationInstance> expired = ArrayListMultimap.create();
    for (ExtensionModel extensionModel : extensions.values()) {
      getConfigurationProviders(extensionModel).stream().filter(provider -> provider instanceof ExpirableConfigurationProvider)
          .forEach(provider -> expired.putAll(provider.getName(), ((ExpirableConfigurationProvider) provider).getExpired()));
    }

    return Multimaps.unmodifiableListMultimap(expired);
  }

}
