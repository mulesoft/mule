/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_INIT_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;

import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.core.api.config.ReconfigurableMuleConfiguration;

import java.util.Map;

/**
 * Takes Mule application descriptor into account when building the context.
 */
public class ApplicationMuleContextBuilder extends SupportsPropertiesMuleContextBuilder {

  private final String appName;
  private final String defaultEncoding;

  public ApplicationMuleContextBuilder(String appName, Map<String, String> appProperties, String defaultEncoding) {
    super(APP, appProperties);
    this.appName = appName;
    this.defaultEncoding = defaultEncoding;
  }

  @Override
  protected DefaultMuleConfiguration createMuleConfiguration() {
    final DefaultMuleConfiguration configuration;
    if (Boolean.valueOf(getArtifactProperties().get(MULE_LAZY_INIT_DEPLOYMENT_PROPERTY))) {
      configuration = new ReconfigurableMuleConfiguration(true);
    } else {
      configuration = new DefaultMuleConfiguration(true);
    }

    initializeFromProperties(configuration);
    configuration.setId(appName);
    final String encoding = defaultEncoding;
    if (!isBlank(encoding)) {
      configuration.setDefaultEncoding(encoding);
    }
    if (executionClassLoader instanceof MuleArtifactClassLoader) {
      configuration
          .setMinMuleVersion(((MuleArtifactClassLoader) executionClassLoader).getArtifactDescriptor().getMinMuleVersion());
    }
    return configuration;
  }
}
