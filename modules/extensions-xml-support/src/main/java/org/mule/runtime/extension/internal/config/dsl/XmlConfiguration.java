/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.config.dsl;

import static java.util.Collections.emptyMap;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.config.ConfigurationState;
import org.mule.runtime.extension.api.runtime.config.ConfigurationStats;
import org.mule.runtime.module.extension.internal.runtime.exception.RequiredParameterNotSetException;

import java.util.Map;
import java.util.Optional;

public class XmlConfiguration extends AbstractComponent implements ConfigurationProvider {

  private final ExtensionModel extensionModel;
  private final ConfigurationModel configurationModel;
  private final MuleContext muleContext;

  public XmlConfiguration(ExtensionModel extensionModel,
                          ConfigurationModel configurationModel,
                          MuleContext muleContext) {
    this.extensionModel = extensionModel;
    this.configurationModel = configurationModel;
    this.muleContext = muleContext;
  }

  private Map<String, Object> properties = emptyMap();

  public Map<String, Object> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, Object> properties) {
    this.properties = properties;
  }

  @Override
  public ConfigurationInstance get(Event event) {
    return new ConfigurationInstance() {

      @Override
      public Object getValue() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public ConfigurationStats getStatistics() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public ConfigurationState getState() {
        return new ConfigurationState() {

          @Override
          public Map<String, Object> getConnectionParameters() {
            return emptyMap();
          }

          @Override
          public Map<String, Object> getConfigParameters() {
            return properties;
          }
        };
      }

      @Override
      public String getName() {
        return XmlConfiguration.this.getName();
      }

      @Override
      public ConfigurationModel getModel() {
        return configurationModel;
      }

      @Override
      public Optional<ConnectionProvider> getConnectionProvider() {
        return Optional.empty();
      }
    };
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
    final String name = configurationModel.getAllParameterModels().stream()
        .filter(ParameterModel::isComponentId)
        .findAny()
        .map(p -> properties.get(p.getName()).toString())
        .orElseThrow(() -> new RequiredParameterNotSetException("cannot create a configuration without a name"));
    return name;
  }

  @Override
  public boolean isDynamic() {
    return false;
  }

}
