/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.artifact.activation.internal;

import static java.util.Objects.requireNonNull;

import org.mule.runtime.module.artifact.activation.api.classloader.ArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginClassLoaderResolver;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderManager;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;


public class TrackingArtifactClassLoaderResolverDecorator implements ArtifactClassLoaderResolver {

  private final ArtifactClassLoaderManager artifactClassLoaderManager;
  private final ArtifactClassLoaderResolver delegate;

  public TrackingArtifactClassLoaderResolverDecorator(ArtifactClassLoaderManager artifactClassLoaderManager,
                                                      ArtifactClassLoaderResolver delegate) {
    this.artifactClassLoaderManager = requireNonNull(artifactClassLoaderManager, "artifactClassLoaderManager");
    this.delegate = requireNonNull(delegate, "delegate");
  }

  @Override
  public MuleDeployableArtifactClassLoader createDomainClassLoader(DomainDescriptor descriptor) {
    return delegate.createDomainClassLoader(descriptor);
  }

  @Override
  public MuleDeployableArtifactClassLoader createDomainClassLoader(DomainDescriptor descriptor,
                                                                   PluginClassLoaderResolver pluginClassLoaderResolver) {
    return delegate.createDomainClassLoader(descriptor, pluginClassLoaderResolver);
  }

  @Override
  public MuleDeployableArtifactClassLoader createApplicationClassLoader(ApplicationDescriptor descriptor,
                                                                        Supplier<ArtifactClassLoader> domainClassLoader) {
    return delegate.createApplicationClassLoader(descriptor, domainClassLoader);
  }

  @Override
  public MuleDeployableArtifactClassLoader createApplicationClassLoader(ApplicationDescriptor descriptor,
                                                                        Supplier<ArtifactClassLoader> domainClassLoader,
                                                                        PluginClassLoaderResolver pluginClassLoaderResolver) {
    return delegate.createApplicationClassLoader(descriptor, domainClassLoader, pluginClassLoaderResolver);
  }

  @Override
  public MuleArtifactClassLoader createMulePluginClassLoader(MuleDeployableArtifactClassLoader ownerArtifactClassLoader,
                                                             ArtifactPluginDescriptor descriptor,
                                                             Function<BundleDescriptor, Optional<ArtifactPluginDescriptor>> pluginDescriptorResolver) {
    MuleArtifactClassLoader mulePluginClassLoader = delegate
        .createMulePluginClassLoader(ownerArtifactClassLoader, descriptor, pluginDescriptorResolver);
    track(mulePluginClassLoader);
    return mulePluginClassLoader;
  }

  private void track(ArtifactClassLoader artifactClassLoader) {
    artifactClassLoaderManager.register(artifactClassLoader);
    artifactClassLoader.addShutdownListener(() -> artifactClassLoaderManager.unregister(artifactClassLoader.getArtifactId()));
  }


}
