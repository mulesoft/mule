/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.extension.api.runtime.config.ConfigurationFactory;

/**
 * A model property which gives access to a {@link ConfigurationFactory}
 *
 * @since 4.0
 */
public final class ConfigurationFactoryModelProperty implements ModelProperty {

  private final ConfigurationFactory configurationFactory;

  public ConfigurationFactoryModelProperty(ConfigurationFactory configurationFactory) {
    this.configurationFactory = configurationFactory;
  }

  public ConfigurationFactory getConfigurationFactory() {
    return configurationFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return "configurationFactoryModelProperty";
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isPublic() {
    return false;
  }
}
