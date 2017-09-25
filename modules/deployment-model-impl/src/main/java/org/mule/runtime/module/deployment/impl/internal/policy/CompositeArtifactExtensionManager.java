/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Composes {@link ExtensionManager} from a child and a parent artifacts, so child artifact can access extensions provided by the
 * parent.
 */
public class CompositeArtifactExtensionManager implements ExtensionManager, Lifecycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(CompositeArtifactExtensionManager.class);

  private final ExtensionManager parentExtensionManager;
  private final ExtensionManager childExtensionManager;
  private final Set<ExtensionModel> extensionModels;

  /**
   * Creates a composed extension manager
   *
   * @param parentExtensionManager extension manager for the parent artifact. Non null
   * @param childExtensionManager extension manager for the child artifact. Non null
   */
  public CompositeArtifactExtensionManager(ExtensionManager parentExtensionManager,
                                           ExtensionManager childExtensionManager) {
    checkArgument(parentExtensionManager != null, "parentExtensionManager cannot be null");
    checkArgument(childExtensionManager != null, "childExtensionManager cannot be null");

    this.parentExtensionManager = parentExtensionManager;
    this.childExtensionManager = childExtensionManager;

    extensionModels = new HashSet<>();
    extensionModels.addAll(parentExtensionManager.getExtensions());
    extensionModels.addAll(childExtensionManager.getExtensions());
  }

  @Override
  public void registerExtension(ExtensionModel extensionModel) {
    throw new UnsupportedOperationException("Composite extension manager cannot register extensions");
  }

  @Override
  public Set<ExtensionModel> getExtensions() {
    return extensionModels;
  }

  @Override
  public Optional<ExtensionModel> getExtension(String extensionName) {
    return extensionModels.stream().filter(extensionModel -> extensionModel.getName().equals(extensionName)).findFirst();
  }

  @Override
  public ConfigurationInstance getConfiguration(String configurationProviderName, CoreEvent event) {
    return getConfigurationProvider(configurationProviderName).map(provider -> provider.get(event))
        .orElseThrow(() -> new IllegalArgumentException(
                                                        format(
                                                               "There is no registered configurationProvider under name '%s'",
                                                               configurationProviderName)));
  }

  @Override
  public Optional<ConfigurationInstance> getConfiguration(ExtensionModel extensionModel, ComponentModel componentModel,
                                                          CoreEvent event) {
    Optional<ConfigurationInstance> configuration = childExtensionManager.getConfiguration(extensionModel, componentModel, event);

    if (configuration.isPresent()) {
      return configuration;
    } else {
      Optional<ConfigurationProvider> provider = getConfigurationProvider(extensionModel, componentModel);
      if (provider.isPresent()) {
        return ofNullable(provider.get().get(event));
      }
    }

    throw new IllegalArgumentException(format(
                                              "There is no registered configuration provider for extension '%s'",
                                              extensionModel.getName()));
  }

  @Override
  public Optional<ConfigurationProvider> getConfigurationProvider(String configurationProviderName) {
    Optional<ConfigurationProvider> configurationProvider =
        childExtensionManager.getConfigurationProvider(configurationProviderName);

    if (!configurationProvider.isPresent()) {
      configurationProvider = parentExtensionManager.getConfigurationProvider(configurationProviderName);
    }

    return configurationProvider;
  }

  public Optional<ConfigurationProvider> getConfigurationProvider(ExtensionModel extensionModel, ComponentModel componentModel) {
    Optional<ConfigurationProvider> configurationModel =
        childExtensionManager.getConfigurationProvider(extensionModel, componentModel);

    if (!configurationModel.isPresent()) {
      configurationModel =
          parentExtensionManager.getConfigurationProvider(extensionModel, componentModel);;
    }

    return configurationModel;
  }

  @Override
  public void registerConfigurationProvider(ConfigurationProvider configurationProvider) {
    throw new UnsupportedOperationException("Composite extension manager cannot register extension providers");
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(childExtensionManager);
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(childExtensionManager);
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(childExtensionManager);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(childExtensionManager, LOGGER);
  }

  public ExtensionManager getParentExtensionManager() {
    return parentExtensionManager;
  }

  public ExtensionManager getChildExtensionManager() {
    return childExtensionManager;
  }
}
