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
import org.mule.runtime.module.artifact.activation.api.plugin.PluginDescriptorResolver;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderManager;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;

import java.net.URL;
import java.util.List;
import java.util.function.Supplier;

/**
 * Tracks {@link ArtifactClassLoader}s created by {@link ArtifactClassLoaderResolver}.
 *
 * @since 4.5
 */
public class TrackingArtifactClassLoaderResolverDecorator implements ArtifactClassLoaderResolver {

  private final ArtifactClassLoaderManager artifactClassLoaderManager;
  private final ArtifactClassLoaderResolver delegate;

  /**
   * Tracks the class loaders created by {@link ArtifactClassLoaderResolver}.
   *
   * @param artifactClassLoaderManager tracks each created class loader. Non-null.
   * @param delegate                   resolver that creates the class loaders to be tracked. Non-null.
   */
  public TrackingArtifactClassLoaderResolverDecorator(ArtifactClassLoaderManager artifactClassLoaderManager,
                                                      ArtifactClassLoaderResolver delegate) {
    this.artifactClassLoaderManager = requireNonNull(artifactClassLoaderManager, "artifactClassLoaderManager");
    this.delegate = requireNonNull(delegate, "delegate");
  }

  @Override
  public MuleDeployableArtifactClassLoader createDomainClassLoader(DomainDescriptor descriptor) {
    MuleDeployableArtifactClassLoader artifactClassLoader = delegate.createDomainClassLoader(descriptor);
    trackDeployableArtifactClassLoader(artifactClassLoader);
    return artifactClassLoader;
  }

  @Override
  public MuleDeployableArtifactClassLoader createDomainClassLoader(DomainDescriptor descriptor,
                                                                   PluginClassLoaderResolver pluginClassLoaderResolver) {
    MuleDeployableArtifactClassLoader artifactClassLoader =
        delegate.createDomainClassLoader(descriptor, pluginClassLoaderResolver);
    trackDeployableArtifactClassLoader(artifactClassLoader);
    return artifactClassLoader;
  }

  @Override
  public MuleDeployableArtifactClassLoader createApplicationClassLoader(ApplicationDescriptor descriptor) {
    MuleDeployableArtifactClassLoader artifactClassLoader = delegate.createApplicationClassLoader(descriptor);
    trackDeployableArtifactClassLoader(artifactClassLoader);
    return artifactClassLoader;
  }

  @Override
  public MuleDeployableArtifactClassLoader createApplicationClassLoader(ApplicationDescriptor descriptor,
                                                                        PluginClassLoaderResolver pluginClassLoaderResolver) {
    MuleDeployableArtifactClassLoader artifactClassLoader =
        delegate.createApplicationClassLoader(descriptor, pluginClassLoaderResolver);
    trackDeployableArtifactClassLoader(artifactClassLoader);
    return artifactClassLoader;
  }

  @Override
  public MuleDeployableArtifactClassLoader createApplicationClassLoader(ApplicationDescriptor descriptor,
                                                                        Supplier<ArtifactClassLoader> domainClassLoader) {
    MuleDeployableArtifactClassLoader artifactClassLoader = delegate.createApplicationClassLoader(descriptor, domainClassLoader);
    trackDeployableArtifactClassLoader(artifactClassLoader);
    return artifactClassLoader;
  }

  @Override
  public MuleDeployableArtifactClassLoader createApplicationClassLoader(ApplicationDescriptor descriptor,
                                                                        Supplier<ArtifactClassLoader> domainClassLoader,
                                                                        PluginClassLoaderResolver pluginClassLoaderResolver) {
    MuleDeployableArtifactClassLoader artifactClassLoader =
        delegate.createApplicationClassLoader(descriptor, domainClassLoader, pluginClassLoaderResolver);
    trackDeployableArtifactClassLoader(artifactClassLoader);
    return artifactClassLoader;
  }

  @Override
  public MuleArtifactClassLoader createMulePluginClassLoader(MuleDeployableArtifactClassLoader ownerArtifactClassLoader,
                                                             ArtifactPluginDescriptor descriptor,
                                                             PluginDescriptorResolver pluginDescriptorResolver) {
    MuleArtifactClassLoader mulePluginClassLoader = delegate
        .createMulePluginClassLoader(ownerArtifactClassLoader, descriptor, pluginDescriptorResolver);
    track(mulePluginClassLoader);
    return mulePluginClassLoader;
  }

  @Override
  public MuleArtifactClassLoader createMulePluginClassLoader(MuleDeployableArtifactClassLoader ownerArtifactClassLoader,
                                                             ArtifactPluginDescriptor descriptor,
                                                             PluginDescriptorResolver pluginDescriptorResolver,
                                                             PluginClassLoaderResolver pluginClassLoaderResolver) {
    MuleArtifactClassLoader mulePluginClassLoader = delegate
        .createMulePluginClassLoader(ownerArtifactClassLoader, descriptor, pluginDescriptorResolver, pluginClassLoaderResolver);
    track(mulePluginClassLoader);
    return mulePluginClassLoader;
  }

  @Override
  public MuleDeployableArtifactClassLoader createApplicationClassLoader(ApplicationDescriptor descriptor,
                                                                        PluginClassLoaderResolver pluginClassLoaderResolver,
                                                                        List<URL> additionalClassloaderUrls) {
    throw new UnsupportedOperationException("Mule Server does not support loading additional classpath entries.");
  }

  @Override
  public MuleDeployableArtifactClassLoader createApplicationClassLoader(ApplicationDescriptor descriptor,
                                                                        Supplier<ArtifactClassLoader> domainClassLoader,
                                                                        PluginClassLoaderResolver pluginClassLoaderResolver,
                                                                        List<URL> additionalClassloaderUrls) {
    throw new UnsupportedOperationException("Mule Server does not support loading additional classpath entries.");
  }

  private void trackDeployableArtifactClassLoader(MuleDeployableArtifactClassLoader artifactClassLoader) {
    artifactClassLoader.getArtifactPluginClassLoaders().forEach(this::track);
    track(artifactClassLoader);
  }

  private void track(ArtifactClassLoader artifactClassLoader) {
    artifactClassLoaderManager.register(artifactClassLoader);
    artifactClassLoader.addShutdownListener(() -> artifactClassLoaderManager.unregister(artifactClassLoader.getArtifactId()));
  }

}
