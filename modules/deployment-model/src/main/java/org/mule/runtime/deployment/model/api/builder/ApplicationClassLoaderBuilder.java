/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.builder;

import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.io.IOException;

/**
 * {@link ArtifactClassLoader} builder for class loaders required by {@link Application} artifacts
 * 
 * @since 4.5
 */
public interface ApplicationClassLoaderBuilder {

  /**
   * @param domain the domain artifact to which the application that is going to use this classloader belongs.
   * @return the builder
   */
  ApplicationClassLoaderBuilder setDomainParentClassLoader(ArtifactClassLoader domainArtifactClassLoader);

  /**
   * @param artifactPluginDescriptors set of plugins descriptors that will be used by the application.
   * @return the builder
   */
  ApplicationClassLoaderBuilder addArtifactPluginDescriptors(ArtifactPluginDescriptor... artifactPluginDescriptors);

  /**
   * @param artifactDescriptor the descriptor of the artifact for which the class loader is going to be created.
   * @return the builder
   */
  ApplicationClassLoaderBuilder setArtifactDescriptor(ArtifactDescriptor artifactDescriptor);

  /**
   * Creates a new {@code MuleDeployableArtifactClassLoader} using the provided configuration. It will create the proper class
   * loader hierarchy and filters so application classes, resources, plugins and it's domain resources are resolve correctly.
   *
   * @return a {@code MuleDeployableArtifactClassLoader} created from the provided configuration.
   * @throws IOException exception cause when it was not possible to access the file provided as dependencies
   */
  MuleDeployableArtifactClassLoader build() throws IOException;

}
