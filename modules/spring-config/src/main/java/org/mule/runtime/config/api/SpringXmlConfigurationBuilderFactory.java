/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api;

import static java.util.Collections.emptyMap;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;

import org.mule.runtime.config.internal.SpringXmlConfigurationBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;

import java.util.Map;

/**
 * @since 4.0
 */
public final class SpringXmlConfigurationBuilderFactory {

  private SpringXmlConfigurationBuilderFactory() {
    // Nothing to do
  }

  public static ConfigurationBuilder createConfigurationBuilder(String configResource) throws ConfigurationException {
    return new SpringXmlConfigurationBuilder(configResource);
  }

  public static ConfigurationBuilder createConfigurationBuilder(String configResource, boolean lazyInit)
      throws ConfigurationException {
    return new SpringXmlConfigurationBuilder(new String[] {configResource}, lazyInit, true);
  }

  public static ConfigurationBuilder createConfigurationBuilder(String[] configResources, boolean lazyInit)
      throws ConfigurationException {
    return new SpringXmlConfigurationBuilder(configResources, lazyInit, true);
  }

  public static ConfigurationBuilder createConfigurationBuilder(String[] configResources, MuleContext domainContext)
      throws ConfigurationException {
    final SpringXmlConfigurationBuilder springXmlConfigurationBuilder =
        new SpringXmlConfigurationBuilder(configResources, emptyMap(), APP, false, false);
    if (domainContext != null) {
      springXmlConfigurationBuilder.setParentContext(domainContext);
    }
    return springXmlConfigurationBuilder;
  }

  public static ConfigurationBuilder createConfigurationBuilder(String configResource, Map<String, String> artifactProperties,
                                                                ArtifactType artifactType)
      throws ConfigurationException {
    return new SpringXmlConfigurationBuilder(configResource, artifactProperties, artifactType);
  }

  public static ConfigurationBuilder createConfigurationBuilder(String[] configResources, Map<String, String> artifactProperties,
                                                                ArtifactType artifactType)
      throws ConfigurationException {
    return new SpringXmlConfigurationBuilder(configResources, artifactProperties, artifactType, false, false);
  }

  public static ConfigurationBuilder createConfigurationBuilder(String configResource, Map<String, String> artifactProperties,
                                                                ArtifactType artifactType, boolean enableLazyInit,
                                                                boolean disableXmlValidations)
      throws ConfigurationException {
    return new SpringXmlConfigurationBuilder(new String[] {configResource}, artifactProperties, artifactType, enableLazyInit,
                                             disableXmlValidations);
  }

  public static ConfigurationBuilder createConfigurationBuilder(String[] configResources, Map<String, String> artifactProperties,
                                                                ArtifactType artifactType, boolean enableLazyInit,
                                                                boolean disableXmlValidations)
      throws ConfigurationException {
    return new SpringXmlConfigurationBuilder(configResources, artifactProperties, artifactType, enableLazyInit,
                                             disableXmlValidations);
  }
}
