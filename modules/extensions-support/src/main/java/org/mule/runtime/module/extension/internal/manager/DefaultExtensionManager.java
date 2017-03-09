/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.manager;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.getConfigurationForComponent;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.requiresConfig;
import static org.mule.runtime.module.extension.internal.manager.DefaultConfigurationExpirationMonitor.Builder.newBuilder;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.time.Time;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.extension.api.manifest.ExtensionManifest;
import org.mule.runtime.extension.api.persistence.manifest.ExtensionManifestXmlSerializer;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.extension.api.util.ExtensionModelUtils;
import org.mule.runtime.module.extension.internal.config.ExtensionConfig;
import org.mule.runtime.module.extension.internal.runtime.config.DefaultImplicitConfigurationProviderFactory;
import org.mule.runtime.module.extension.internal.runtime.config.ImplicitConfigurationProviderFactory;
import org.mule.runtime.module.extension.internal.runtime.exception.TooManyConfigsException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link ExtensionManager}. This implementation uses standard Java SPI as a discovery mechanism.
 * <p/>
 * Although it allows registering {@link ConfigurationProvider} instances through the
 * {@link #registerConfigurationProvider(ConfigurationProvider)} method (and that's still the correct way of registering them),
 * this implementation automatically acknowledges any {@link ConfigurationProvider} already present on the {@link MuleRegistry}
 *
 * @since 3.7.0
 */
public final class DefaultExtensionManager implements ExtensionManager, MuleContextAware, Initialisable, Startable, Stoppable {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultExtensionManager.class);
  public static final String TOO_MANY_CONFIGS =
      "Too many configs of type '%s' were found. Please specify one which one should be used.";

  private final ImplicitConfigurationProviderFactory implicitConfigurationProviderFactory =
      new DefaultImplicitConfigurationProviderFactory();

  private MuleContext muleContext;
  private ExtensionRegistry extensionRegistry;
  private ConfigurationExpirationMonitor configurationExpirationMonitor;
  private ExtensionErrorsRegistrant extensionErrorsRegistrant;

  @Override
  public void initialise() throws InitialisationException {
    extensionRegistry = new ExtensionRegistry(muleContext.getRegistry());
    extensionErrorsRegistrant =
        new ExtensionErrorsRegistrant(muleContext.getErrorTypeRepository(), muleContext.getErrorTypeLocator());
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
  }

  /**
   * Stops the {@link #configurationExpirationMonitor}
   *
   * @throws MuleException if it fails to stop
   */
  @Override
  public void stop() throws MuleException {
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
        extensionErrorsRegistrant.registerErrors(extensionModel);
      });
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerConfigurationProvider(ConfigurationProvider configurationProvider) {
    extensionRegistry.registerConfigurationProvider(configurationProvider);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConfigurationInstance getConfiguration(String configurationProviderName, Event muleEvent) {
    return getConfigurationProvider(configurationProviderName).map(provider -> provider.get(muleEvent))
        .orElseThrow(() -> new IllegalArgumentException(
                                                        format(
                                                               "There is no registered configurationProvider under name '%s'",
                                                               configurationProviderName)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConfigurationInstance getConfiguration(ExtensionModel extensionModel, ComponentModel componentModel,
                                                Event muleEvent) {

    Optional<ConfigurationProvider> provider = getConfigurationProvider(extensionModel, componentModel);
    if (provider.isPresent()) {
      return provider.get().get(muleEvent);
    }

    Optional<ConfigurationModel> configurationModel =
        getConfigurationModelForExtension(extensionModel, getConfigurationForComponent(extensionModel,
                                                                                       componentModel));
    if (!configurationModel.isPresent()) {
      return null;
    }

    createImplicitConfiguration(extensionModel, configurationModel.get(), muleEvent);
    return getConfiguration(extensionModel, componentModel, muleEvent);
  }

  @Override
  public Optional<ConfigurationProvider> getConfigurationProvider(ExtensionModel extensionModel, ComponentModel componentModel) {
    Optional<ConfigurationModel> config = getConfigurationModelForExtension(extensionModel,
                                                                            getConfigurationForComponent(extensionModel,
                                                                                                         componentModel));
    if (!config.isPresent() && requiresConfig(extensionModel, componentModel)) {
      throw new IllegalStateException("No config-ref was specified for component '%s' of extension '%s'. Please specify which to use");
    }

    return config.map(c -> getConfigurationProvider(extensionModel, c)).orElse(empty());
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
                                           Event muleEvent) {
    synchronized (extensionModel) {
      // check that another thread didn't beat us to create the instance
      if (extensionRegistry.getConfigurationProviders(extensionModel, implicitConfigurationModel).isEmpty()) {
        registerConfigurationProvider(implicitConfigurationProviderFactory.createImplicitConfigurationProvider(extensionModel,
                                                                                                               implicitConfigurationModel,
                                                                                                               muleEvent,
                                                                                                               muleContext));
      }
    }
  }

  private Optional<ConfigurationProvider> getConfigurationProvider(ExtensionModel extensionModel,
                                                                   ConfigurationModel configurationModel) {
    Collection<ConfigurationProvider> providers = extensionRegistry.getConfigurationProviders(extensionModel, configurationModel);

    int matches = providers.size();

    if (matches == 1) {
      return providers.stream().findFirst();
    } else if (matches > 1) {
      throw new TooManyConfigsException("Too many configs of type '" + configurationModel.getName() + "' found for ",
                                        extensionModel, configurationModel, matches);
    }

    return empty();
  }

  private Optional<ConfigurationModel> getConfigurationModelForExtension(ExtensionModel extensionModel,
                                                                         Set<ConfigurationModel> assignableConfigurationModels) {
    Reference<ConfigurationModel> selectedConfig = new Reference<>();

    List<ConfigurationModel> registeredProviders = extensionRegistry.getConfigurationProviders(extensionModel).stream()
        .map(ConfigurationProvider::getConfigurationModel).collect(Collectors.toList());

    assignableConfigurationModels.forEach(c -> {
      List<ConfigurationModel> candidateConfigs =
          registeredProviders.stream().filter(r -> Objects.equals(r.getName(), c.getName())).collect(Collectors.toList());

      if (candidateConfigs.size() == 1) {
        if (selectedConfig.get() == null) {
          selectedConfig.set(c);
        } else {
          throw new TooManyConfigsException(format(TOO_MANY_CONFIGS, c.getName()), extensionModel, c, 2);
        }
      } else if (candidateConfigs.size() > 1) {
        throw new TooManyConfigsException(format(TOO_MANY_CONFIGS, c.getName()), extensionModel, c, candidateConfigs.size());
      }
    });

    if (selectedConfig.get() == null) {
      List<ConfigurationModel> implicitConfigurationModels =
          assignableConfigurationModels.stream().filter(ExtensionModelUtils::canBeUsedImplicitly).collect(Collectors.toList());

      if (implicitConfigurationModels.size() == 1) {
        selectedConfig.set(implicitConfigurationModels.get(0));
      } else if (implicitConfigurationModels.size() > 1) {
        throw new IllegalStateException(format("No configuration can be inferred por extension '%s'", extensionModel.getName()));
      }
    }

    return ofNullable(selectedConfig.get());
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

  /**
   * {@inheritDoc}
   */
  @Override
  public ExtensionManifest parseExtensionManifestXml(URL manifestUrl) {
    try (InputStream manifestStream = manifestUrl.openStream()) {
      return new ExtensionManifestXmlSerializer().deserialize(IOUtils.toString(manifestStream));
    } catch (IOException e) {
      throw new MuleRuntimeException(createStaticMessage("Could not read extension manifest on plugin " + manifestUrl.toString()),
                                     e);
    }
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
    ExtensionConfig extensionConfig = muleContext.getConfiguration().getExtension(ExtensionConfig.class);
    final Time defaultFreq = new Time(5L, TimeUnit.MINUTES);

    if (extensionConfig != null) {
      return extensionConfig.getDynamicConfigExpirationFrequency().orElse(defaultFreq);
    } else {
      return defaultFreq;
    }
  }

  @Override
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }
}
