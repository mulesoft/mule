/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.classloader;

import static org.mule.runtime.module.artifact.activation.internal.classloader.ArtifactClassLoaderResolverConstants.CONTAINER_CLASS_LOADER;
import static org.mule.runtime.module.artifact.activation.internal.classloader.ArtifactClassLoaderResolverConstants.MODULE_REPOSITORY;

import static java.nio.file.Files.createTempDirectory;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.container.api.ModuleRepository;
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
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Provides a way to create classLoaders for different artifact types.
 *
 * @since 4.5
 */
@NoImplement
// TODO W-10914518 - Test with native libraries at all levels: domain, app, plugin.
public interface ArtifactClassLoaderResolver {

  static ArtifactClassLoaderResolver defaultClassLoaderResolver() {
    return classLoaderResolver(CONTAINER_CLASS_LOADER,
                               MODULE_REPOSITORY,
                               name -> {
                                 try {
                                   return createTempDirectory("nativeLibs_" + name).toFile();
                                 } catch (IOException e) {
                                   throw new IllegalStateException(e);
                                 }
                               });
  }

  static ArtifactClassLoaderResolver classLoaderResolver(ArtifactClassLoader containerClassLoader,
                                                         ModuleRepository moduleRepository,
                                                         Function<String, File> tempFolderChildFunction) {
    return new DefaultArtifactClassLoaderResolver(containerClassLoader,
                                                  moduleRepository,
                                                  new DefaultNativeLibraryFinderFactory(tempFolderChildFunction));
  }

  /**
   * Creates a class loader for a domain. This will create the class loader itself and all of its internal required state:
   * regionClassLoader, classLoaders for plugins.
   * </p>
   * The given descriptor must have its dependencies' exported packages sanitized (i.e. dependencies must not have packages that
   * are already exported by their transitive dependencies nor any other dependency).
   *
   * @param descriptor the descriptor of the domain to generate a class loader for.
   * @return a class loader for a domain.
   */
  MuleDeployableArtifactClassLoader createDomainClassLoader(DomainDescriptor descriptor);

  /**
   * Creates a class loader for a domain. This will create the class loader itself and all of its internal required state:
   * regionClassLoader, classLoaders for plugins.
   * </p>
   * The given descriptor must have its dependencies' exported packages sanitized (i.e. dependencies must not have packages that
   * are already exported by their transitive dependencies nor any other dependency).
   *
   * @param descriptor                the descriptor of the domain to generate a class loader for.
   * @param pluginClassLoaderResolver allows the user to provide a class loader for the given plugin, otherwise it will be
   *                                  created.
   * @return a class loader for a domain.
   */
  MuleDeployableArtifactClassLoader createDomainClassLoader(DomainDescriptor descriptor,
                                                            PluginClassLoaderResolver pluginClassLoaderResolver);

  /**
   * Creates a class loader for a domain. This will create the class loader itself and all of its internal required state:
   * regionClassLoader, classLoaders for plugins.
   * </p>
   * The given descriptor must have its dependencies' exported packages sanitized (i.e. dependencies must not have packages that
   * are already exported by their transitive dependencies nor any other dependency).
   *
   * @param descriptor                the descriptor of the domain to generate a class loader for.
   * @param pluginClassLoaderResolver allows the user to provide a class loader for the given plugin, otherwise it will be
   *                                  created.
   * @param additionalClassloaderUrls a list of {@link URL} pointing to additional resources and classes
   * @return a class loader for a domain.
   *
   * @since 4.7
   */
  MuleDeployableArtifactClassLoader createDomainClassLoader(DomainDescriptor descriptor,
                                                            PluginClassLoaderResolver pluginClassLoaderResolver,
                                                            List<URL> additionalClassloaderUrls);

  /**
   * Creates a class loader for an application. This will create the class loader itself and all of its internal required state:
   * regionClassLoader, classLoaders for plugins.
   * </p>
   * The given descriptor must have its dependencies' exported packages sanitized (i.e. dependencies must not have packages that
   * are already exported by their transitive dependencies nor any other dependency).
   *
   * @param descriptor the descriptor of the application to generate a class loader for.
   * @return a class loader for an application.
   */
  MuleDeployableArtifactClassLoader createApplicationClassLoader(ApplicationDescriptor descriptor);

  /**
   * Creates a class loader for an application. This will create the class loader itself and all of its internal required state:
   * regionClassLoader, classLoaders for plugins.
   * </p>
   * The given descriptor must have its dependencies' exported packages sanitized (i.e. dependencies must not have packages that
   * are already exported by their transitive dependencies nor any other dependency).
   *
   * @param descriptor                the descriptor of the application to generate a class loader for.
   * @param pluginClassLoaderResolver allows the user to provide a class loader for the given plugin, otherwise it will be
   *                                  created.
   * @return a class loader for an application.
   */
  MuleDeployableArtifactClassLoader createApplicationClassLoader(ApplicationDescriptor descriptor,
                                                                 PluginClassLoaderResolver pluginClassLoaderResolver);

  /**
   * Creates a class loader for an application. This will create the class loader itself and all of its internal required state:
   * regionClassLoader, classLoaders for plugins.
   * </p>
   * The given descriptor must have its dependencies' exported packages sanitized (i.e. dependencies must not have packages that
   * are already exported by their transitive dependencies nor any other dependency).
   *
   * @param descriptor        the descriptor of the application to generate a class loader for.
   * @param domainClassLoader the class loader of the domain the application belongs to.
   * @return a class loader for an application.
   */
  MuleDeployableArtifactClassLoader createApplicationClassLoader(ApplicationDescriptor descriptor,
                                                                 Supplier<ArtifactClassLoader> domainClassLoader);

  /**
   * Creates a class loader for an application. This will create the class loader itself and all of its internal required state:
   * regionClassLoader, classLoaders for plugins.
   * </p>
   * The given descriptor must have its dependencies' exported packages sanitized (i.e. dependencies must not have packages that
   * are already exported by their transitive dependencies nor any other dependency).
   *
   * @param descriptor                the descriptor of the application to generate a class loader for.
   * @param domainClassLoader         the class loader of the domain the application belongs to.
   * @param pluginClassLoaderResolver allows the user to provide a class loader for the given plugin, otherwise it will be
   *                                  created.
   * @return a class loader for an application.
   */
  MuleDeployableArtifactClassLoader createApplicationClassLoader(ApplicationDescriptor descriptor,
                                                                 Supplier<ArtifactClassLoader> domainClassLoader,
                                                                 PluginClassLoaderResolver pluginClassLoaderResolver);

  /**
   * Creates a class loader for an application. This will create the class loader itself and all of its internal required state:
   * regionClassLoader, classLoaders for plugins.
   * </p>
   * The given descriptor must have its dependencies' exported packages sanitized (i.e. dependencies must not have packages that
   * are already exported by their transitive dependencies nor any other dependency).
   *
   * @param descriptor                the descriptor of the application to generate a class loader for.
   * @param pluginClassLoaderResolver allows the user to provide a class loader for the given plugin, otherwise it will be
   *                                  created.
   * @param additionalClassloaderUrls a list of {@link URL} pointing to additional resources and classes
   * @return a class loader for an application.
   */
  MuleDeployableArtifactClassLoader createApplicationClassLoader(ApplicationDescriptor descriptor,
                                                                 PluginClassLoaderResolver pluginClassLoaderResolver,
                                                                 List<URL> additionalClassloaderUrls);

  /**
   * Creates a class loader for an application. This will create the class loader itself and all of its internal required state:
   * regionClassLoader, classLoaders for plugins.
   * </p>
   * The given descriptor must have its dependencies' exported packages sanitized (i.e. dependencies must not have packages that
   * are already exported by their transitive dependencies nor any other dependency).
   *
   * @param descriptor                the descriptor of the application to generate a class loader for.
   * @param domainClassLoader         the class loader of the domain the application belongs to.
   * @param pluginClassLoaderResolver allows the user to provide a class loader for the given plugin, otherwise it will be
   *                                  created.
   * @param additionalClassloaderUrls a list of {@link URL} pointing to additional resources and classes
   * @return a class loader for an application.
   */
  MuleDeployableArtifactClassLoader createApplicationClassLoader(ApplicationDescriptor descriptor,
                                                                 Supplier<ArtifactClassLoader> domainClassLoader,
                                                                 PluginClassLoaderResolver pluginClassLoaderResolver,
                                                                 List<URL> additionalClassloaderUrls);

  /**
   * Creates a class loader for a plugin.
   * <p>
   * The class loader for a plugin is based on the class loader of its owner artifact for some scenarios regarding exported
   * packages/resources. For that reason, a class loader for a plugin in one application may be different from the same plugin in
   * another application.
   *
   * @param ownerArtifactClassLoader the class loader for the artifact that has the plugin dependency for the target class loader.
   * @param descriptor               the descriptor of the plugin to generate a class loader for.
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
   * Creates a class loader for a plugin.
   * <p>
   * The class loader for a plugin is based on the class loader of its owner artifact for some scenarios regarding exported
   * packages/resources. For that reason, a class loader for a plugin in one application may be different from the same plugin in
   * another application.
   *
   * @param ownerArtifactClassLoader  the class loader for the artifact that has the plugin dependency for the target class
   *                                  loader.
   * @param descriptor                the descriptor of the plugin to generate a class loader for.
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
