/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config.builders;

import static org.mule.runtime.core.api.config.i18n.CoreMessages.configurationBuilderSuccess;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.objectIsNull;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigResource;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.util.StringUtils;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;

/**
 * Abstract {@link ConfigurationBuilder} implementation used for ConfigurationBuider's that use one of more configuration
 * resources of the same type that are defined using strings or {@link ConfigResource} objects. It is recommended that
 * {@link ConfigResource} objects are used over strings since they can be more descriptive, but Strings will be supported for
 * quite some time.
 */
public abstract class AbstractResourceConfigurationBuilder extends AbstractConfigurationBuilder {

  private static final Logger LOGGER = getLogger(AbstractResourceConfigurationBuilder.class);

  private final Map<String, String> artifactProperties;
  protected ConfigResource[] artifactConfigResources;

  /**
   * @param artifactConfigResources a comma separated list of configuration files to load, this should be accessible on the
   *        classpath or filesystem
   * @param artifactProperties map of properties that can be referenced from the {@code artifactConfigResources} as external
   *        configuration values
   * @throws org.mule.runtime.core.api.config.ConfigurationException usually if the config resources cannot be loaded
   */
  public AbstractResourceConfigurationBuilder(String artifactConfigResources, Map<String, String> artifactProperties)
      throws ConfigurationException {
    this.artifactConfigResources = loadConfigResources(StringUtils.splitAndTrim(artifactConfigResources, ",; "));
    this.artifactProperties = artifactProperties;
  }

  /**
   * @param artifactConfigResources an array of configuration files to load, this should be accessible on the classpath or
   *        filesystem
   * @param artifactProperties map of properties that can be referenced from the {@code artifactConfigResources} as external
   *        configuration values
   * @throws org.mule.runtime.core.api.config.ConfigurationException usually if the config resources cannot be loaded
   */
  public AbstractResourceConfigurationBuilder(String[] artifactConfigResources, Map<String, String> artifactProperties)
      throws ConfigurationException {
    this.artifactConfigResources = loadConfigResources(artifactConfigResources);
    this.artifactProperties = artifactProperties;
  }

  /**
   * @param artifactConfigResources an array Reader oject that provides acces to a configuration either locally or remotely
   * @param artifactProperties map of properties that can be referenced from the {@code artifactConfigResources} as external
   *        configuration values
   */
  public AbstractResourceConfigurationBuilder(ConfigResource[] artifactConfigResources, Map<String, String> artifactProperties) {
    this.artifactConfigResources = artifactConfigResources;
    this.artifactProperties = artifactProperties;
  }

  /**
   * Override to check for existence of configResouce before invocation, and set resources n configuration afterwards.
   */
  @Override
  public void configure(MuleContext muleContext) throws ConfigurationException {
    if (artifactConfigResources == null) {
      throw new ConfigurationException(objectIsNull("Configuration Resources"));
    }

    super.configure(muleContext);

    LOGGER.debug(configurationBuilderSuccess(this, createConfigResourcesString()).toString());
  }

  protected ConfigResource[] loadConfigResources(String[] configs) throws ConfigurationException {
    try {
      artifactConfigResources = new ConfigResource[configs.length];
      for (int i = 0; i < configs.length; i++) {
        artifactConfigResources[i] = new ConfigResource(configs[i]);
      }
      return artifactConfigResources;
    } catch (IOException e) {
      throw new ConfigurationException(e);
    }
  }

  protected String createConfigResourcesString() {
    StringBuilder configResourcesString = new StringBuilder();
    configResourcesString.append("[");
    for (int i = 0; i < artifactConfigResources.length; i++) {
      configResourcesString.append(artifactConfigResources[i]);
      if (i < artifactConfigResources.length - 1) {
        configResourcesString.append(", ");
      }
    }
    configResourcesString.append("]");
    return configResourcesString.toString();
  }

  public Map<String, String> getArtifactProperties() {
    return artifactProperties;
  }
}
