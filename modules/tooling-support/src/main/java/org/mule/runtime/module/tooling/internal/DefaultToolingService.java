/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal;

import static java.util.Optional.of;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_MUTE_APP_LOGS_DEPLOYMENT_PROPERTY;

import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.tooling.api.ToolingService;
import org.mule.runtime.module.tooling.api.connectivity.ConnectivityTestingServiceBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Default implementation of {@code ToolingService}.
 *
 * @since 4.0
 */
public class DefaultToolingService implements ToolingService {

  private final DefaultApplicationFactory applicationFactory;

  /**
   * @param applicationFactory factory for creating the {@link Application}
   */
  public DefaultToolingService(DefaultApplicationFactory applicationFactory) {
    this.applicationFactory = applicationFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectivityTestingServiceBuilder newConnectivityTestingServiceBuilder() {
    return new DefaultConnectivityTestingServiceBuilder(applicationFactory);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Application createApplication(File applicationLocation) throws IOException {
    Application application = applicationFactory.createArtifact(applicationLocation, of(createApplicationProperties()));
    application.install();
    application.lazyInit();
    application.start();
    return application;
  }

  private static Properties createApplicationProperties() {
    Properties properties = new Properties();
    properties.setProperty(MULE_MUTE_APP_LOGS_DEPLOYMENT_PROPERTY, "true");
    return properties;
  }
}
