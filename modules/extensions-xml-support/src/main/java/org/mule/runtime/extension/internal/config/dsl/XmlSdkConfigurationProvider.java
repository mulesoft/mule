/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.extension.internal.config.dsl;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;

import java.util.Map;

/**
 * {@link ConfigurationProvider} implementation for Xml-Sdk connectors.
 *
 * @since 4.3
 */
public class XmlSdkConfigurationProvider extends AbstractComponent implements ConfigurationProvider {

  private final String name;
  private final Map<String, String> parameters;
  private final ExtensionModel extensionModel;
  private final ConfigurationModel configurationModel;
  private final Registry registry;

  public XmlSdkConfigurationProvider(String name,
                                     Map<String, String> parameters,
                                     ExtensionModel extensionModel,
                                     ConfigurationModel configurationModel,
                                     Registry registry) {
    this.name = name;
    this.parameters = parameters;
    this.extensionModel = extensionModel;
    this.configurationModel = configurationModel;
    this.registry = registry;
  }


  public Map<String, String> getParameters() {
    return parameters;
  }

  @Override
  public ConfigurationInstance get(Event event) {
    return new XmlSdkCompositeConfigurationInstance(name, configurationModel, event, registry);
  }

  @Override
  public boolean isDynamic() {
    return false;
  }

  @Override
  public ExtensionModel getExtensionModel() {
    return extensionModel;
  }

  @Override
  public ConfigurationModel getConfigurationModel() {
    return configurationModel;
  }

  @Override
  public String getName() {
    return this.name;
  }

}
