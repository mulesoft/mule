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
import org.mule.runtime.module.artifact.activation.api.plugin.PluginClassLoaderResolver;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginDescriptorResolver;
import org.mule.runtime.module.artifact.activation.internal.classloader.DefaultArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.activation.internal.nativelib.DefaultNativeLibraryFinderFactory;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Provides a way to create classLoaders for different artifact types.
 * 
 * @since 4.5
 */
// TODO W-10914518 - Test with native libraries at all levels: domain, app, plugin.
public interface ArtifactClassLoaderResolver {

  static ArtifactClassLoaderResolver defaultClassLoaderResolver() {
    return classLoaderResolver(new DefaultModuleRepository(new ContainerModuleDiscoverer(ArtifactClassLoaderResolver.class
        .getClassLoader())),
                               name -> {
                                 try {
                                   return createTempDirectory("nativeLibs_" + name).toFile();
                                 } catch (IOException e) {
                                   throw new IllegalStateException(e);
                                 }
                               });
  }

  static ArtifactClassLoaderResolver classLoaderResolver(ModuleRepository moduleRepository,
                                                         Function<String, File> tempFolderChildFunction) {
    return new DefaultArtifactClassLoaderResolver(moduleRepository,
                                                  new DefaultNativeLibraryFinderFactory(tempFolderChildFunction));
  }

  /**
   * Creates a classLoader for a domain. This will create the classLoader itself and all of its internal required state:
   * regionClassLoader, classLoaders for plugins.
   *
   * @param descriptor the descriptor of the domain to generate a classLoader for.
   * @return a classLoader for a domain.
   */
  MuleDeployableArtifactClassLoader createDomainClassLoader(DomainDescriptor descriptor);

  /**
   * Creates a classLoader for a domain. This will create the classLoader itself and all of its internal required state:
   * regionClassLoader, classLoaders for plugins.
   * 
   * @param descriptor                the descriptor of the domain to generate a classLoader for.
   * @param pluginClassLoaderResolver allows the user to provide a class loader for the given plugin, otherwise it will be
   *                                  created.
   * @return a classLoader for a domain.
   */
  MuleDeployableArtifactClassLoader createDomainClassLoader(DomainDescriptor descriptor,
                                                            PluginClassLoaderResolver pluginClassLoaderResolver);

  /**
   * Creates a classLoader for an application. This will create the classLoader itself and all of its internal required state:
   * regionClassLoader, classLoaders for plugins.
   * 
   * @param descriptor        the descriptor of the application to generate a classLoader for.
   * @param domainClassLoader the class loader of the domain the application belongs to.
   * @return a classLoader for an application.
   */
  MuleDeployableArtifactClassLoader createApplicationClassLoader(ApplicationDescriptor descriptor,
                                                                 Supplier<ArtifactClassLoader> domainClassLoader);

  /**
   * Creates a classLoader for an application. This will create the classLoader itself and all of its internal required state:
   * regionClassLoader, classLoaders for plugins.
   *
   * @param descriptor                the descriptor of the application to generate a classLoader for.
   * @param domainClassLoader         the class loader of the domain the application belongs to.
   * @param pluginClassLoaderResolver allows the user to provide a class loader for the given plugin, otherwise it will be
   *                                  created.
   * @return a classLoader for an application.
   */
  MuleDeployableArtifactClassLoader createApplicationClassLoader(ApplicationDescriptor descriptor,
                                                                 Supplier<ArtifactClassLoader> domainClassLoader,
                                                                 PluginClassLoaderResolver pluginClassLoaderResolver);

  /**
   * Creates a classLoader for a plugin.
   * <p>
   * The classLoader for a plugin is based on the classLoader of its owner artifact for some scenarios regarding exported
   * packages/resources. For that reason, a classLoader for a plugin in one application may be different from the same plugin in
   * another application.
   *
   * @param ownerArtifactClassLoader the classLoader for the artifact that has the plugin dependency for the target classLoader.
   * @param descriptor               the descriptor of the plugin to generate a classLoader for.
   * @param pluginDescriptorResolver a wrapper function around the logic to extract an {@link ArtifactPluginDescriptor} from the
   *                                 jar described by the {@link BundleDescriptor}. The function must return
   *                                 {@link Optional#empty()} if the plugin represented by the {@link BundleDescriptor} is not a
   *                                 dependency of the artifact for {@code ownerArtifactClassLoader}.
   * @return a classloader for a plugin within a given application or domain.
   */
  MuleArtifactClassLoader createMulePluginClassLoader(MuleDeployableArtifactClassLoader ownerArtifactClassLoader,
                                                      ArtifactPluginDescriptor descriptor,
                                                      PluginDescriptorResolver pluginDescriptorResolver);

  /**
   * Creates a classLoader for a plugin.
   * <p>
   * The classLoader for a plugin is based on the classLoader of its owner artifact for some scenarios regarding exported
   * packages/resources. For that reason, a classLoader for a plugin in one application may be different from the same plugin in
   * another application.
   * 
   * @param ownerArtifactClassLoader  the classLoader for the artifact that has the plugin dependency for the target classLoader.
   * @param descriptor                the descriptor of the plugin to generate a classLoader for.
   * @param pluginDescriptorResolver  a wrapper function around the logic to extract an {@link ArtifactPluginDescriptor} from the
   *                                  jar described by the {@link BundleDescriptor}. The function must return
   *                                  {@link Optional#empty()} if the plugin represented by the {@link BundleDescriptor} is not a
   *                                  dependency of the artifact for {@code ownerArtifactClassLoader}.
   * @param pluginClassLoaderResolver allows the user to provide a class loader for the given dependency plugin, otherwise it will
   *                                  be obtained from the owner artifact class loaders.
   * @return a classloader for a plugin within a given application or domain.
   */
  MuleArtifactClassLoader createMulePluginClassLoader(MuleDeployableArtifactClassLoader ownerArtifactClassLoader,
                                                      ArtifactPluginDescriptor descriptor,
                                                      PluginDescriptorResolver pluginDescriptorResolver,
                                                      PluginClassLoaderResolver pluginClassLoaderResolver);

}
