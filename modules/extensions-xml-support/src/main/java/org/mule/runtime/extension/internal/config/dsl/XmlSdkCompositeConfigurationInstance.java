/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.config.dsl;

import static org.mule.runtime.extension.internal.ast.MacroExpansionModuleModel.MODULE_CONNECTION_GLOBAL_ELEMENT_NAME;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.config.ConfigurationState;
import org.mule.runtime.extension.api.runtime.config.ConfigurationStats;
import org.mule.runtime.extension.internal.ast.property.TestConnectionGlobalElementModelProperty;

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
  private final LazyValue<Optional<ConfigurationInstance>> actualConfigurationInstance;

  public XmlSdkCompositeConfigurationInstance(String name,
                                              ConfigurationModel model,
                                              Event event,
                                              Registry registry) {
    this.name = name;
    this.model = model;
    this.actualConfigurationInstance = new LazyValue<>(
                                                       () -> model
                                                           .getConnectionProviderModel(MODULE_CONNECTION_GLOBAL_ELEMENT_NAME)
                                                           .flatMap(connProviderModel -> connProviderModel
                                                               .getModelProperty(TestConnectionGlobalElementModelProperty.class))
                                                           .map(connTester -> connTester.getGlobalElementName() + "-" + getName())
                                                           .flatMap(registry::<ConfigurationProvider>lookupByName)
                                                           .map(cp -> cp.get(event)));
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
    return actualConfigurationInstance.get().flatMap(ConfigurationInstance::getConnectionProvider);
  }

  @Override
  public Object getValue() {
    return actualConfigurationInstance.get().map(ConfigurationInstance::getValue).orElse(null);
  }

  @Override
  public ConfigurationStats getStatistics() {
    return actualConfigurationInstance.get().map(ConfigurationInstance::getStatistics).orElse(null);
  }

  @Override
  public ConfigurationState getState() {
    return actualConfigurationInstance.get().map(ConfigurationInstance::getState).orElse(null);
  }

}
