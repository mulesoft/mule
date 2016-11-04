/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.application;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.toFile;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.deployment.internal.artifact.ArtifactStartedSplashScreen;

import java.util.Set;

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
    listItems(stream(descriptor.getClassLoaderModel().getUrls()).map(url -> toFile(url).getName()).collect(toList()),
              "Application libraries:");
  }
}
