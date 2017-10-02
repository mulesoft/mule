/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.manager;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.getConfigurationForComponent;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.requiresConfig;
import static org.mule.runtime.module.extension.internal.manager.DefaultConfigurationExpirationMonitor.Builder.newBuilder;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getImplicitConfigurationProviderName;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.time.Time;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.core.internal.registry.DefaultRegistry;
import org.mule.runtime.core.privileged.PrivilegedMuleContext;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.util.ExtensionModelUtils;
import org.mule.runtime.module.extension.internal.runtime.config.DefaultImplicitConfigurationProviderFactory;
import org.mule.runtime.module.extension.internal.runtime.config.ImplicitConfigurationProviderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link ExtensionManager}. This implementation uses standard Java SPI as a discovery mechanism.
 * <p/>
 * Although it allows registering {@link ConfigurationProvider} instances through the
 * {@link #registerConfigurationProvider(ConfigurationProvider)} method (and that's still the correct way of registering them),
 * this implementation automatically acknowledges any {@link ConfigurationProvider} already present on the {@link Registry}
 *
 * @since 3.7.0
 */
public final class DefaultExtensionManager implements ExtensionManager, MuleContextAware, Initialisable, Startable, Stoppable {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultExtensionManager.class);

  private final ImplicitConfigurationProviderFactory implicitConfigurationProviderFactory =
      new DefaultImplicitConfigurationProviderFactory();

  private MuleContext muleContext;
  private ExtensionRegistry extensionRegistry;
  private ConfigurationExpirationMonitor configurationExpirationMonitor;
  private ExtensionErrorsRegistrant extensionErrorsRegistrant;

  private ExtensionActivator extensionActivator;

  @Override
  public void initialise() throws InitialisationException {
    extensionRegistry = new ExtensionRegistry(new DefaultRegistry(muleContext));
    extensionErrorsRegistrant =
        new ExtensionErrorsRegistrant(muleContext.getErrorTypeRepository(),
                                      ((PrivilegedMuleContext) muleContext).getErrorTypeLocator());
    extensionActivator = new ExtensionActivator(extensionErrorsRegistrant, muleContext);
  }

  /**
   * Starts the {@link #configurationExpirationMonitor}
   *
   * @throws MuleException if it fails to start
   */
  @Override
  public void start() throws MuleException {
    configurationExpirationMonitor = newConfigurationExpirationMonitor();
    configurationExpirationMonitor.beginMonitoring();
    extensionActivator.start();
  }

  /**
   * Stops the {@link #configurationExpirationMonitor}
   *
   * @throws MuleException if it fails to stop
   */
  @Override
  public void stop() throws MuleException {
    extensionActivator.stop();
    configurationExpirationMonitor.stopMonitoring();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerExtension(ExtensionModel extensionModel) {
    final String extensionName = extensionModel.getName();
    final String extensionVersion = extensionModel.getVersion();
    final String extensionVendor = extensionModel.getVendor();

    LOGGER.info("Registering extension {} (version: {} vendor: {} )", extensionName, extensionVersion, extensionVendor);

    if (extensionRegistry.containsExtension(extensionName)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("An extension of name '{}' (version: {} vendor {}) is already registered. Skipping...", extensionName,
                     extensionVersion, extensionVendor);
      }
    } else {
      withContextClassLoader(getClassLoader(extensionModel), () -> {
        extensionRegistry.registerExtension(extensionName, extensionModel);
        extensionActivator.activateExtension(extensionModel);
      });
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerConfigurationProvider(ConfigurationProvider configurationProvider) {
    extensionRegistry.registerConfigurationProvider(configurationProvider, muleContext);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConfigurationInstance getConfiguration(String configurationProviderName, CoreEvent muleEvent) {
    return getConfigurationProvider(configurationProviderName).map(provider -> provider.get(muleEvent))
        .orElseThrow(() -> new IllegalArgumentException(format("There is no registered configurationProvider under name '%s'",
                                                               configurationProviderName)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ConfigurationInstance> getConfiguration(ExtensionModel extensionModel, ComponentModel componentModel,
                                                          CoreEvent muleEvent) {

    ConfigurationInstance instance =
        getConfigurationProvider(extensionModel, componentModel).map(p -> p.get(muleEvent)).orElse(null);
    if (instance != null) {
      return of(instance);
    }

    Optional<ConfigurationModel> configurationModel =
        getConfigurationModelForExtension(extensionModel, getConfigurationForComponent(extensionModel,
                                                                                       componentModel));
    if (configurationModel.isPresent()) {
      createImplicitConfiguration(extensionModel, configurationModel.get(), muleEvent);
      return of(getConfiguration(getImplicitConfigurationProviderName(extensionModel, configurationModel.get()),
                                 muleEvent));
    }

    return empty();
  }

  @Override
  public Optional<ConfigurationProvider> getConfigurationProvider(ExtensionModel extensionModel, ComponentModel componentModel) {
    Optional<ConfigurationModel> config = getConfigurationModelForExtension(extensionModel,
                                                                            getConfigurationForComponent(extensionModel,
                                                                                                         componentModel));
    if (!config.isPresent() && requiresConfig(extensionModel, componentModel)) {
      throw new IllegalStateException(
                                      format("No config-ref was specified for component '%s' of extension '%s'. Please specify which to use",
                                             componentModel.getName(), extensionModel.getName()));
    }

    return config.map(c -> getConfigurationProvider(getImplicitConfigurationProviderName(extensionModel, c))).orElse(empty());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ConfigurationProvider> getConfigurationProvider(String configurationProviderName) {
    checkArgument(!StringUtils.isBlank(configurationProviderName), "cannot get configuration from a blank provider name");
    return extensionRegistry.getConfigurationProvider(configurationProviderName);
  }

  private void createImplicitConfiguration(ExtensionModel extensionModel, ConfigurationModel implicitConfigurationModel,
                                           CoreEvent muleEvent) {
    String implicitConfigurationProviderName = getImplicitConfigurationProviderName(extensionModel, implicitConfigurationModel);
    if (!extensionRegistry
        .getConfigurationProvider(implicitConfigurationProviderName)
        .isPresent()) {
      synchronized (extensionModel) {
        // check that another thread didn't beat us to create the instance
        if (!extensionRegistry
            .getConfigurationProvider(implicitConfigurationProviderName)
            .isPresent()) {
          registerConfigurationProvider(implicitConfigurationProviderFactory.createImplicitConfigurationProvider(extensionModel,
                                                                                                                 implicitConfigurationModel,
                                                                                                                 muleEvent,
                                                                                                                 muleContext));
        }
      }
    }
  }

  private Optional<ConfigurationModel> getConfigurationModelForExtension(ExtensionModel extensionModel,
                                                                         Set<ConfigurationModel> assignableConfigurationModels) {
    List<ConfigurationModel> implicitConfigurationModels =
        assignableConfigurationModels.stream().filter(ExtensionModelUtils::canBeUsedImplicitly).collect(Collectors.toList());

    if (implicitConfigurationModels.isEmpty()) {
      return empty();
    } else if (implicitConfigurationModels.size() == 1) {
      return ofNullable(implicitConfigurationModels.get(0));
    }

    throw new IllegalStateException(format("No configuration can be inferred for extension '%s'", extensionModel.getName()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<ExtensionModel> getExtensions() {
    return extensionRegistry.getExtensions();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ExtensionModel> getExtension(String extensionName) {
    return extensionRegistry.getExtension(extensionName);
  }

  private ConfigurationExpirationMonitor newConfigurationExpirationMonitor() {
    Time freq = getConfigurationExpirationFrequency();
    return newBuilder(extensionRegistry, muleContext).runEvery(freq.getTime(), freq.getUnit())
        .onExpired((key, object) -> disposeConfiguration(key, object)).build();
  }

  private void disposeConfiguration(String key, ConfigurationInstance configuration) {
    try {
      stopIfNeeded(configuration);
      disposeIfNeeded(configuration, LOGGER);
    } catch (Exception e) {
      LOGGER.error(format("Could not dispose expired dynamic config of key '%s' and type %s", key,
                          configuration.getClass().getName()),
                   e);
    }
  }

  private Time getConfigurationExpirationFrequency() {
    return muleContext.getConfiguration().getDynamicConfigExpiration().getFrequency();
  }

  @Override
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  public ExtensionActivator getExtensionActivator() {
    return extensionActivator;
  }
}
