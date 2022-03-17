/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.classloader;

import static java.nio.file.Files.createTempDirectory;

import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.internal.ContainerModuleDiscoverer;
import org.mule.runtime.container.internal.DefaultModuleRepository;
import org.mule.runtime.module.artifact.activation.internal.classloader.DefaultArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.activation.internal.nativelib.DefaultNativeLibraryFinderFactory;
import org.mule.runtime.module.artifact.activation.internal.nativelib.NativeLibraryFinderFactory;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;

import java.io.IOException;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Provides a way to create classLoaders for different artifact types.
 * 
 * @since 4.5
 */
// TODO Check that the plugin classLoader cache works fine for things like db if the actual driver changes.
// TODO Test with native libraries at all levels: domain, app, plugin.
public interface ArtifactClassLoaderResolver {

  static ArtifactClassLoaderResolver defaultClassLoaderResolver() {
    return new DefaultArtifactClassLoaderResolver(new DefaultModuleRepository(new ContainerModuleDiscoverer(ArtifactClassLoaderResolver.class
        .getClassLoader())),
                                                  new DefaultNativeLibraryFinderFactory(name -> {
                                                    try {
                                                      return createTempDirectory("nativeLibs_" + name).toFile();
                                                    } catch (IOException e) {
                                                      throw new IllegalStateException(e);
                                                    }
                                                  }));
  }

  static ArtifactClassLoaderResolver classLoaderResolver(ModuleRepository moduleRepository,
                                                         NativeLibraryFinderFactory nativeLibraryFinderFactory) {
    return new DefaultArtifactClassLoaderResolver(moduleRepository, nativeLibraryFinderFactory);
  }

  /**
   * Creates a classLoader for a domain. This will create the classLoader itself and all of its internal required state:
   * regionClassLoader, classLoaders for plugins.
   * 
   * @param descriptor                the descriptor of the domain to generate a classLoader for.
   * @param pluginClassLoaderResolver a wrapper function around
   *                                  {@link #createMulePluginClassLoader(MuleDeployableArtifactClassLoader, ArtifactPluginDescriptor, Function, BiFunction)}.
   *                                  This is usually just a call to
   *                                  {@link #resolvePluginClassLoader(ArtifactClassLoader, ArtifactPluginDescriptor)}.
   * @return a classLoader for a domain.
   */
  MuleDeployableArtifactClassLoader createDomainClassLoader(DomainDescriptor descriptor,
                                                            BiFunction<ArtifactClassLoader, ArtifactPluginDescriptor, ArtifactClassLoader> pluginClassLoaderResolver);

  /**
   * Creates a classLoader for an application. This will create the classLoader itself and all of its internal required state:
   * regionClassLoader, classLoaders for plugins.
   * 
   * @param descriptor                the descriptor of the application to generate a classLoader for.
   * @param domainClassLoaderResolver a wrapper function for {@link #createDomainClassLoader(DomainDescriptor, BiFunction)}. An
   *                                  {@link Optional#empty()} input means that the application does not use a domain.
   * @param pluginClassLoaderResolver a wrapper function around
   *                                  {@link #createMulePluginClassLoader(MuleDeployableArtifactClassLoader, ArtifactPluginDescriptor, Function, BiFunction)}.
   *                                  This is usually just a call to
   *                                  {@link #resolvePluginClassLoader(ArtifactClassLoader, ArtifactPluginDescriptor)}.
   * @return a classLoader for an application.
   */
  MuleDeployableArtifactClassLoader createApplicationClassLoader(ApplicationDescriptor descriptor,
                                                                 Function<Optional<BundleDescriptor>, MuleDeployableArtifactClassLoader> domainClassLoaderResolver,
                                                                 BiFunction<ArtifactClassLoader, ArtifactPluginDescriptor, ArtifactClassLoader> pluginClassLoaderResolver);

  /**
   * Creates a classLoader for a plugin.
   * <p>
   * The classLoader for a plugin is based on the classLoader of its owner artifact for some scenarios regarding exported
   * packages/resources. For that reason, a classLoader for a plugin in one application may be different than the same plugin in
   * another application.
   * 
   * @param ownerArtifactClassLoader  the classLoader for the artifact that has the plugin dependency for the target classLoader.
   * @param descriptor                the descriptor of the plugin to generate a classLoader for.
   * @param pluginDescriptorResolver  a wrapper function aroun d the logic to extract an {@link ArtifactPluginDescriptor} from the
   *                                  jar described by the {@link BundleDescriptor}. The function must return
   *                                  {@link Optional#empty()} if the plugin represented by the {@link BundleDescriptor} is not a
   *                                  dependency of the artifact for {@code ownerArtifactClassLoader}.
   * @param pluginClassLoaderResolver a wrapper function around
   *                                  {@link #createMulePluginClassLoader(MuleDeployableArtifactClassLoader, ArtifactPluginDescriptor, Function, BiFunction)}.
   *                                  This is needed to resolve classLoaders for plugins that are dependencies of other plugins.
   *                                  This is usually just a call to
   *                                  {@link #resolvePluginClassLoader(ArtifactClassLoader, ArtifactPluginDescriptor)}.
   * @return a classloader for a plugin within a given application or domain.
   */
  MuleArtifactClassLoader createMulePluginClassLoader(MuleDeployableArtifactClassLoader ownerArtifactClassLoader,
                                                      ArtifactPluginDescriptor descriptor,
                                                      Function<BundleDescriptor, Optional<ArtifactPluginDescriptor>> pluginDescriptorResolver,
                                                      BiFunction<ArtifactClassLoader, ArtifactPluginDescriptor, ArtifactClassLoader> pluginClassLoaderResolver);

  /**
   * Creates a classLoader for a plugin.
   * <p>
   * The classLoader for a plugin is based on the classLoader of its owner artifact for some scenarios regarding exported
   * packages/resources. For that reason, a classLoader for a plugin in one application may be different than the same plugin in
   * another application.
   * 
   * @param ownerArtifactClassLoader the classLoader for the artifact that has the plugin dependency for the target classLoader.
   * @param descriptor               the descriptor of the plugin to generate a classLoader for.
   * @return a classloader for a plugin within a given application or domain.
   */
  MuleArtifactClassLoader resolvePluginClassLoader(ArtifactClassLoader ownerClassLoader, ArtifactPluginDescriptor descriptor);

}
