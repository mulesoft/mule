/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.processors;

import org.mule.runtime.api.component.ConfigurationProperties;

import java.util.Properties;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

/**
 * TODO
 */
public class PropertyPlaceholderProcessor extends PropertyPlaceholderConfigurer {

  private ConfigurationProperties configurationProperties;

  @Override
  protected String resolvePlaceholder(String placeholder, Properties props, int systemPropertiesMode) {
    return configurationProperties.resolveStringProperty(placeholder).orElse(null);
  }

  @Override
  protected String resolvePlaceholder(String placeholder, Properties props) {
    return configurationProperties.resolveStringProperty(placeholder).orElse(null);
  }

  public void setConfigurationProperties(ConfigurationProperties configurationProperties) {
    this.configurationProperties = configurationProperties;
  }
}
