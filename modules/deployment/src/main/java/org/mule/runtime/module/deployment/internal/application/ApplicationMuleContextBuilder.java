/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.application;

import static org.mule.runtime.core.config.bootstrap.ArtifactType.APP;
import org.mule.runtime.core.DefaultMuleContext;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.config.DefaultMuleConfiguration;
import org.mule.runtime.core.config.PropertiesMuleConfigurationFactory;
import org.mule.runtime.core.context.DefaultMuleContextBuilder;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.core.work.MuleWorkManager;

import java.util.Map;

/**
 * Takes Mule application descriptor into account when building the context.
 */
public class ApplicationMuleContextBuilder extends DefaultMuleContextBuilder {

  private final Map<String, String> appProperties;
  private final String appName;
  private final String defaultEncoding;

  public ApplicationMuleContextBuilder(String appName, Map<String, String> appProperties, String defaultEncoding) {
    this.appProperties = appProperties;
    this.appName = appName;
    this.defaultEncoding = defaultEncoding;
  }

  @Override
  protected DefaultMuleContext createDefaultMuleContext() {
    DefaultMuleContext muleContext = super.createDefaultMuleContext();
    muleContext.setArtifactType(APP);
    return muleContext;
  }

  @Override
  protected DefaultMuleConfiguration createMuleConfiguration() {
    final DefaultMuleConfiguration configuration = new DefaultMuleConfiguration(true);
    PropertiesMuleConfigurationFactory.initializeFromProperties(configuration, appProperties);
    configuration.setId(appName);
    final String encoding = defaultEncoding;
    if (StringUtils.isNotBlank(encoding)) {
      configuration.setDefaultEncoding(encoding);
    }
    return configuration;
  }

  @Override
  protected MuleWorkManager createWorkManager() {
    // use app name in the core Mule thread
    final String threadName = String.format("[%s].Mule", appName);
    return new MuleWorkManager(ThreadingProfile.DEFAULT_THREADING_PROFILE, threadName,
                               getMuleConfiguration().getShutdownTimeout());

  }
}
