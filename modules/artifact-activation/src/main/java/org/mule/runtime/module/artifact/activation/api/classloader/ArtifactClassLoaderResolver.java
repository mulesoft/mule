/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.classloader;

import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 
 * @since 4.5
 */
// TODO Check that the plugin classloader cache works fine for things like db if the actual driver changes.
// TODO Test with native libraries at all levels: domain, app, plugin.
public interface ArtifactClassLoaderResolver {

  MuleDeployableArtifactClassLoader createDomainClassLoader(DomainDescriptor descriptor,
                                                            BiFunction<ArtifactClassLoader, ArtifactPluginDescriptor, ArtifactClassLoader> pluginClassLoaderResolver);

  MuleDeployableArtifactClassLoader createApplicationClassLoader(ApplicationDescriptor descriptor,
                                                                 Function<Optional<BundleDescriptor>, MuleDeployableArtifactClassLoader> domainClassLoaderResolver,
                                                                 BiFunction<ArtifactClassLoader, ArtifactPluginDescriptor, ArtifactClassLoader> pluginClassLoaderResolver);

  MuleArtifactClassLoader createMulePluginClassLoader(MuleDeployableArtifactClassLoader ownerArtifactClassLoader,
                                                      ArtifactPluginDescriptor descriptor,
                                                      Function<BundleDescriptor, Optional<ArtifactPluginDescriptor>> pluginDescriptorResolver,
                                                      BiFunction<ArtifactClassLoader, ArtifactPluginDescriptor, ArtifactClassLoader> pluginClassLoaderResolver);

}
