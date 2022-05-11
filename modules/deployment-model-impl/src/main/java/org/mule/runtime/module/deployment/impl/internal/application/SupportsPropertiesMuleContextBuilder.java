/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static org.apache.commons.beanutils.BeanUtils.setProperty;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.internal.context.DefaultMuleContextBuilder;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Takes Mule artifact descriptor into account when building the context.
 *
 * @since 4.1
 */
abstract class SupportsPropertiesMuleContextBuilder extends DefaultMuleContextBuilder {

  private static final String SYS_PROPERTY_PREFIX = "sys.";
  private static final String MULE_PROPERTY_PREFIX = "mule.config.";

  private final Map<String, String> artifactProperties;

  /**
   * Constructs a new {@link MuleContext} builder with the given {@code artifactProperties}.
   *
   * @param artifactType       the type of artifact the target {@link MuleContext} is for.
   * @param artifactProperties The properties of the artifact.
   */
  protected SupportsPropertiesMuleContextBuilder(ArtifactType artifactType, Map<String, String> artifactProperties) {
    super(artifactType);
    this.artifactProperties = artifactProperties;
  }

  protected void initializeFromProperties(MuleConfiguration configuration) {
    for (Map.Entry<String, String> entry : artifactProperties.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();

      if (key.startsWith(SYS_PROPERTY_PREFIX)) {
        String systemProperty = key.substring(SYS_PROPERTY_PREFIX.length());
        System.setProperty(systemProperty, value);
      } else if (key.startsWith(MULE_PROPERTY_PREFIX)) {
        String configProperty = key.substring(MULE_PROPERTY_PREFIX.length());
        try {
          setProperty(configuration, configProperty, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
          logger.error("Cannot set configuration property", e);
        }
      }
    }
  }

  protected Map<String, String> getArtifactProperties() {
    return artifactProperties;
  }
}
