/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static org.apache.commons.beanutils.BeanUtils.setProperty;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;

import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.internal.context.DefaultMuleContextBuilder;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Takes Mule application descriptor into account when building the context.
 */
public class ApplicationMuleContextBuilder extends DefaultMuleContextBuilder {

  private static final String SYS_PROPERTY_PREFIX = "sys.";
  private static final String MULE_PROPERTY_PREFIX = "mule.config.";

  private final Map<String, String> appProperties;
  private final String appName;
  private final String defaultEncoding;

  public ApplicationMuleContextBuilder(String appName, Map<String, String> appProperties, String defaultEncoding) {
    super(APP);
    this.appProperties = appProperties;
    this.appName = appName;
    this.defaultEncoding = defaultEncoding;
  }

  @Override
  protected DefaultMuleConfiguration createMuleConfiguration() {
    final DefaultMuleConfiguration configuration = new DefaultMuleConfiguration(true);
    initializeFromProperties(configuration, appProperties);
    configuration.setId(appName);
    final String encoding = defaultEncoding;
    if (!isBlank(encoding)) {
      configuration.setDefaultEncoding(encoding);
    }
    return configuration;
  }

  private static void initializeFromProperties(MuleConfiguration configuration, Map<String, String> properties) {
    for (Map.Entry<String, String> entry : properties.entrySet()) {
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
}
