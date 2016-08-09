/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.application;

import static org.mule.runtime.module.launcher.MuleFoldersUtil.getAppLibFolder;
import org.mule.runtime.module.launcher.artifact.ArtifactStartedSplashScreen;
import org.mule.runtime.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginDescriptor;

import java.util.Set;

/**
 * Splash screen specific for {@link Application} startup based on it's {@link ApplicationDescriptor}.
 */
public class ApplicationStartedSplashScreen extends ArtifactStartedSplashScreen<ApplicationDescriptor> {

  @Override
  protected void createMessage(ApplicationDescriptor descriptor) {
    doBody(String.format("Started app '%s'", descriptor.getName()));
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
        doBody(String.format(VALUE_FORMAT, plugin.getName()));
      }
    }
  }

  protected void listLibraries(ApplicationDescriptor descriptor) {
    listItems(getLibraries(getAppLibFolder(descriptor.getName())), "Application libraries:");
  }
}
