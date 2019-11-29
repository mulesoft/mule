/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.config.dsl;

import static org.mule.runtime.config.internal.dsl.model.extension.xml.MacroExpansionModuleModel.MODULE_CONNECTION_GLOBAL_ELEMENT_NAME;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.config.internal.dsl.model.extension.xml.property.TestConnectionGlobalElementModelProperty;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.config.ConfigurationState;
import org.mule.runtime.extension.api.runtime.config.ConfigurationStats;

import java.util.List;
import java.util.Optional;

/**
 * {@link ConfigurationInstance} implementation for Xml-Sdk connectors, that will delegate its connectivity to the internal config
 * connectionProvider indicated by the {@link TestConnectionGlobalElementModelProperty} from the extension model.
 * <p>
 * Methods other than {@link #getConnectionProvider()}, {@link #getModel()} and {@link #getName()} are no-ops.
 *
 * @since 4.3
 */
class XmlSdkCompositeConfigurationInstance implements ConfigurationInstance {

  private final String name;
  private final ConfigurationModel model;
  private final List<ConfigurationProvider> innerConfigProviders;
  private final Event event;

  public XmlSdkCompositeConfigurationInstance(String name, ConfigurationModel model,
                                              List<ConfigurationProvider> innerConfigProviders, Event event) {
    this.name = name;
    this.model = model;
    this.innerConfigProviders = innerConfigProviders;
    this.event = event;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public ConfigurationModel getModel() {
    return model;
  }

  @Override
  public Optional<ConnectionProvider> getConnectionProvider() {
    final Optional<String> testConnectionGlobalElement = model.getConnectionProviderModel(MODULE_CONNECTION_GLOBAL_ELEMENT_NAME)
        .flatMap(connProviderModel -> connProviderModel.getModelProperty(TestConnectionGlobalElementModelProperty.class))
        .map(connTester -> connTester.getGlobalElementName() + "-" + getName());

    return innerConfigProviders
        .stream()
        .filter(icp -> testConnectionGlobalElement.map(globalElementName -> globalElementName.equals(icp.getName())).orElse(true))
        .map(icp -> icp.get(event).getConnectionProvider())
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }

  // No-op methods:

  @Override
  public Object getValue() {
    return null;
  }

  @Override
  public ConfigurationStats getStatistics() {
    return null;
  }

  @Override
  public ConfigurationState getState() {
    return null;
  }

}
