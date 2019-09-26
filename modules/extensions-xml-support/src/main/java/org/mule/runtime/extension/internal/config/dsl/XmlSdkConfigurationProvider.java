/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.config.dsl;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;

import java.util.List;
import java.util.Map;

/**
 * {@link ConfigurationProvider} implementation for Xml-Sdk connectors.
 *
 * @since 4.3
 */
public class XmlSdkConfigurationProvider extends AbstractComponent implements ConfigurationProvider {

  private final String name;
  private final List<ConfigurationProvider> innerConfigProviders;
  private final Map<String, String> parameters;
  private final ExtensionModel extensionModel;
  private final ConfigurationModel configurationModel;

  public XmlSdkConfigurationProvider(String name,
                                     List<ConfigurationProvider> innerConfigProviders,
                                     Map<String, String> parameters,
                                     ExtensionModel extensionModel,
                                     ConfigurationModel configurationModel) {
    this.name = name;
    this.innerConfigProviders = innerConfigProviders;
    this.parameters = parameters;
    this.extensionModel = extensionModel;
    this.configurationModel = configurationModel;
  }


  public Map<String, String> getParameters() {
    return parameters;
  }

  @Override
  public ConfigurationInstance get(Event event) {
    return new XmlSdkCompositeConfigurationInstance(name, configurationModel, innerConfigProviders, event);
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
