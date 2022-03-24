/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.artifact;

import static org.mule.runtime.core.internal.config.bootstrap.ClassLoaderRegistryBootstrapDiscoverer.BOOTSTRAP_PROPERTIES;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.bootstrap.BootstrapService;
import org.mule.runtime.core.api.config.bootstrap.BootstrapServiceDiscoverer;
import org.mule.runtime.core.api.config.bootstrap.PropertiesBootstrapService;
import org.mule.runtime.core.api.config.bootstrap.PropertiesBootstrapServiceDiscoverer;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.util.PropertiesUtils;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;

import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Configures a {@link BootstrapServiceDiscoverer} on an artifact's {@link MuleContext}
 * <p/>
 * Configuration done by this builder will use a {@link BootstrapServiceDiscoverer} to find {@link BootstrapService} provided by
 * the container and will create a {@link BootstrapServiceDiscoverer} for each plugin deployed in the artifact. All the discovered
 * services will then used to configure the context.
 */
public class ArtifactBootstrapServiceDiscovererConfigurationBuilder extends AbstractConfigurationBuilder {

  private final List<ArtifactPlugin> artifactPlugins;

  /**
   * Creates a new context builder
   *
   * @param artifactPlugins artifact plugins deployed inside an artifact. Non null.
   */
  public ArtifactBootstrapServiceDiscovererConfigurationBuilder(List<ArtifactPlugin> artifactPlugins) {
    checkArgument(artifactPlugins != null, "ArtifactPlugins cannot be null");
    this.artifactPlugins = artifactPlugins;
  }

  @Override
  protected void doConfigure(MuleContext muleContext) throws Exception {
    final PropertiesBootstrapServiceDiscoverer propertiesBootstrapServiceDiscoverer =
        new PropertiesBootstrapServiceDiscoverer(this.getClass().getClassLoader());

    List<BootstrapService> bootstrapServices = new LinkedList<>();
    bootstrapServices.addAll(propertiesBootstrapServiceDiscoverer.discover());

    for (ArtifactPlugin artifactPlugin : artifactPlugins) {
      final Enumeration<URL> resources = artifactPlugin.getArtifactClassLoader().findResources(BOOTSTRAP_PROPERTIES);

      while (resources.hasMoreElements()) {
        final URL localResource = resources.nextElement();
        final Properties properties = PropertiesUtils.loadProperties(localResource);
        final BootstrapService pluginBootstrapService =
            new PropertiesBootstrapService(artifactPlugin.getArtifactClassLoader().getClassLoader(), properties);

        bootstrapServices.add(pluginBootstrapService);
      }
    }

    muleContext.setBootstrapServiceDiscoverer(() -> bootstrapServices);
  }
}
