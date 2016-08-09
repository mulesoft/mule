/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.manager;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.module.extension.internal.manager.DefaultConfigurationExpirationMonitor.Builder.newBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.core.time.Time;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.extension.api.introspection.ExtensionFactory;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.RuntimeExtensionModel;
import org.mule.runtime.extension.api.introspection.declaration.spi.Describer;
import org.mule.runtime.extension.api.manifest.ExtensionManifest;
import org.mule.runtime.extension.api.persistence.manifest.ExtensionManifestXmlSerializer;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.config.ExtensionConfig;
import org.mule.runtime.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.runtime.module.extension.internal.runtime.config.DefaultImplicitConfigurationProviderFactory;
import org.mule.runtime.module.extension.internal.runtime.config.ImplicitConfigurationProviderFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Default implementation of {@link ExtensionManagerAdapter}. This implementation uses standard Java SPI as a discovery mechanism.
 * <p/>
 * Although it allows registering {@link ConfigurationProvider} instances through the
 * {@link #registerConfigurationProvider(ConfigurationProvider)} method (and that's still the correct way of registering them),
 * this implementation automatically acknowledges any {@link ConfigurationProvider} already present on the {@link MuleRegistry}
 *
 * @since 3.7.0
 */
public final class DefaultExtensionManager
    implements ExtensionManagerAdapter, MuleContextAware, Initialisable, Startable, Stoppable {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultExtensionManager.class);

  private final ServiceRegistry serviceRegistry = new SpiServiceRegistry();
  private final ImplicitConfigurationProviderFactory implicitConfigurationProviderFactory =
      new DefaultImplicitConfigurationProviderFactory();
  private final DescriberResolver describerResolver = new DescriberResolver();

  private MuleContext muleContext;
  private ExtensionRegistry extensionRegistry;
  private ExtensionFactory extensionFactory;
  private ConfigurationExpirationMonitor configurationExpirationMonitor;

  @Override
  public void initialise() throws InitialisationException {
    extensionRegistry = new ExtensionRegistry(muleContext.getRegistry());
    extensionFactory = new DefaultExtensionFactory(serviceRegistry, muleContext.getExecutionClassLoader());
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
  public void registerExtension(RuntimeExtensionModel extensionModel) {
    final String extensionName = extensionModel.getName();
    final String extensionVersion = extensionModel.getVersion();
    final String extensionVendor = extensionModel.getVendor();

    LOGGER.info("Registering extension {} (version: {} vendor: {} )", extensionName, extensionVersion, extensionVendor);

    if (extensionRegistry.containsExtension(extensionName, extensionVendor)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("A extension of name '{}' (version: {} vendor {}) is already registered. Skipping...", extensionName,
                     extensionVersion, extensionVendor);
      }
    } else {
      extensionRegistry.registerExtension(extensionName, extensionVendor, extensionModel);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerExtension(ExtensionManifest manifest, ClassLoader classLoader) {
    Describer describer = describerResolver.resolve(manifest, classLoader);
    final DefaultDescribingContext context = new DefaultDescribingContext(classLoader);

    RuntimeExtensionModel extensionModel =
        withContextClassLoader(classLoader, () -> extensionFactory.createFrom(describer.describe(context), context));

    registerExtension(extensionModel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <C> void registerConfigurationProvider(ConfigurationProvider<C> configurationProvider) {
    extensionRegistry.registerConfigurationProvider(configurationProvider);
  }

  /**
   * {@inheritDoc}
   */
  // TODO: MULE-8946
  @Override
  public <C> ConfigurationInstance<C> getConfiguration(String configurationProviderName, Object muleEvent) {
    return (ConfigurationInstance<C>) getConfigurationProvider(configurationProviderName).map(provider -> provider.get(muleEvent))
        .orElseThrow(() -> new IllegalArgumentException(String
            .format("There is no registered configurationProvider under name '%s'", configurationProviderName)));
  }

  /**
   * {@inheritDoc}
   */
  // TODO: MULE-8946
  @Override
  public <C> ConfigurationInstance<C> getConfiguration(ExtensionModel extensionModel, Object muleEvent) {
    Optional<ConfigurationProvider<C>> provider = getConfigurationProvider(extensionModel);
    if (provider.isPresent()) {
      return provider.get().get(muleEvent);
    }

    createImplicitConfiguration(extensionModel, (MuleEvent) muleEvent);
    return getConfiguration(extensionModel, muleEvent);
  }

  public <C> Optional<ConfigurationProvider<C>> getConfigurationProvider(ExtensionModel extensionModel) {
    List<ConfigurationProvider> providers = extensionRegistry.getConfigurationProviders(extensionModel);

    int matches = providers.size();

    if (matches == 1) {
      return Optional.of(providers.get(0));
    } else if (matches > 1) {
      throw new IllegalStateException(String.format(
                                                    "No config-ref was specified for operation of extension '%s', but %d are registered. Please specify which to use",
                                                    extensionModel.getName(), matches));
    }

    return Optional.empty();
  }

  public <C> Optional<ConfigurationProvider<C>> getConfigurationProvider(String configurationProviderName) {
    checkArgument(!StringUtils.isBlank(configurationProviderName), "cannot get configuration from a blank provider name");
    return extensionRegistry.getConfigurationProvider(configurationProviderName);
  }

  private void createImplicitConfiguration(ExtensionModel extensionModel, MuleEvent muleEvent) {
    synchronized (extensionModel) {
      // check that another thread didn't beat us to create the instance
      if (extensionRegistry.getConfigurationProviders(extensionModel).isEmpty()) {
        registerConfigurationProvider(implicitConfigurationProviderFactory.createImplicitConfigurationProvider(extensionModel,
                                                                                                               muleEvent));
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<RuntimeExtensionModel> getExtensions() {
    return extensionRegistry.getExtensions();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<RuntimeExtensionModel> getExtensions(String extensionName) {
    return extensionRegistry.getExtensions(extensionName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<RuntimeExtensionModel> getExtension(String extensionName, String vendor) {
    return extensionRegistry.getExtension(extensionName, vendor);
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

  private void disposeConfiguration(String key, ConfigurationInstance<Object> configuration) {
    try {
      stopIfNeeded(configuration);
      disposeIfNeeded(configuration, LOGGER);
    } catch (Exception e) {
      LOGGER.error(String.format("Could not dispose expired dynamic config of key '%s' and type %s", key,
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
