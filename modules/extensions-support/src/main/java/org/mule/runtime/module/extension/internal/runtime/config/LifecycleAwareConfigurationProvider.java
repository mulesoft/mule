/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.lifecycle.DefaultLifecycleManager;
import org.mule.runtime.core.lifecycle.SimpleLifecycleManager;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * Base class for implementations of {@link ConfigurationProvider} which keep track of the {@link ConfigurationInstance} they
 * generate and propagate lifecycle and IoC into them.
 * <p/>
 * It also implements the other common concerns of every {@link ConfigurationProvider}, leaving implementations with the need to
 * &quot;just&quot; implement {@link #get(Event)}
 *
 * @since 4.0
 */
public abstract class LifecycleAwareConfigurationProvider implements ConfigurationProvider, Lifecycle {

  private static final Logger LOGGER = getLogger(LifecycleAwareConfigurationProvider.class);

  private final String name;
  private final ExtensionModel extensionModel;
  private final ConfigurationModel configurationModel;
  private final List<ConfigurationInstance> configurationInstances = new LinkedList<>();
  private final ClassLoader extensionClassLoader;
  protected SimpleLifecycleManager lifecycleManager =
      new DefaultLifecycleManager<>(String.format("%s-%s", getClass().getName(), getName()), this);

  @Inject
  protected MuleContext muleContext;

  public LifecycleAwareConfigurationProvider(String name, ExtensionModel extensionModel, ConfigurationModel configurationModel) {
    this.name = name;
    this.extensionModel = extensionModel;
    this.configurationModel = configurationModel;
    extensionClassLoader = getClassLoader(extensionModel);
  }

  /**
   * Performs dependency injection into all the currently provided configurations, and when needed, fires the
   * {@link Initialisable#initialise()} phase on them
   *
   * @throws InitialisationException if an exception is found
   */
  @Override
  public void initialise() throws InitialisationException {
    withContextClassLoader(extensionClassLoader, () -> {
      lifecycleManager.fireInitialisePhase((phaseName, object) -> {
        for (ConfigurationInstance configurationInstance : configurationInstances) {
          initialiseIfNeeded(configurationInstance, true, muleContext);
        }
      });
      return null;
    }, InitialisationException.class, e -> {
      throw new InitialisationException(e, this);
    });
  }

  /**
   * When needed, fires the {@link Startable#start()} phase on the currently provided configurations
   *
   * @throws MuleException if an exception is found
   */
  @Override
  public void start() throws MuleException {
    withContextClassLoader(extensionClassLoader, () -> {
      lifecycleManager.fireStartPhase((phaseName, object) -> {
        for (ConfigurationInstance configurationInstance : configurationInstances) {
          startConfig(configurationInstance);
        }
      });
      return null;
    }, MuleException.class, e -> {
      throw new DefaultMuleException(e);
    });
  }

  /**
   * When needed, fires the {@link Stoppable#stop()} phase on the currently provided configurations
   *
   * @throws MuleException if an exception is found
   */
  @Override
  public void stop() throws MuleException {
    withContextClassLoader(extensionClassLoader, () -> {
      lifecycleManager.fireStopPhase((phaseName, object) -> {
        for (ConfigurationInstance configurationInstance : configurationInstances) {
          stopIfNeeded(configurationInstance);
        }
      });
      return null;
    }, MuleException.class, e -> {
      throw new DefaultMuleException(e);
    });
  }

  /**
   * When needed, fires the {@link Disposable#dispose()} phase on the currently provided configurations
   */
  @Override
  public void dispose() {
    try {
      withContextClassLoader(extensionClassLoader, () -> {
        lifecycleManager.fireDisposePhase((phaseName, object) -> {
          for (ConfigurationInstance configurationInstance : configurationInstances) {
            disposeIfNeeded(configurationInstance, LOGGER);
          }
        });
        return null;
      });
    } catch (Exception e) {
      LOGGER.error("Could not dispose configuration provider of name " + getName(), e);
    }
  }

  /**
   * Implementations are to invoke this method everytime they create a new {@link ConfigurationInstance} so that they're kept
   * track of and the lifecycle can be propagated
   *
   * @param configuration a newly created {@link ConfigurationInstance}
   */
  protected void registerConfiguration(ConfigurationInstance configuration) {
    configurationInstances.add(configuration);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConfigurationModel getConfigurationModel() {
    return configurationModel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ExtensionModel getExtensionModel() {
    return extensionModel;
  }

  protected void startConfig(ConfigurationInstance config) throws MuleException {
    startIfNeeded(config);
  }

  protected ClassLoader getExtensionClassLoader() {
    return extensionClassLoader;
  }
}
