/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.application;

import org.mule.runtime.module.deployment.api.application.Application;
import org.mule.runtime.module.deployment.internal.artifact.ArtifactStartedSplashScreen;
import org.mule.runtime.module.deployment.internal.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.deployment.internal.plugin.ArtifactPluginDescriptor;

import java.util.Set;

import static java.lang.String.format;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppLibFolder;

/**
 * Splash screen specific for {@link Application} startup based on it's {@link ApplicationDescriptor}.
 */
public class ApplicationStartedSplashScreen extends ArtifactStartedSplashScreen<ApplicationDescriptor> {

  @Override
  public void createMessage(ApplicationDescriptor descriptor) {
    doBody(format("Started app '%s'", descriptor.getName()));
    if (RUNTIME_VERBOSE_PROPERTY.isEnabled()) {
      listPlugins(descriptor);
      listLibraries(descriptor);
    }
  }

  private void listPlugins(ApplicationDescriptor descriptor) {
    Set<ArtifactPluginDescriptor> plugins = descriptor.getPlugins();
    if (!plugins.isEmpty()) {
      doBody("Application plugins:");
      for (ArtifactPluginDescriptor plugin : plugins) {
        doBody(format(VALUE_FORMAT, plugin.getName()));
      }
    }
  }

  protected void listLibraries(ApplicationDescriptor descriptor) {
    listItems(getLibraries(getAppLibFolder(descriptor.getName())), "Application libraries:");
  }
}
