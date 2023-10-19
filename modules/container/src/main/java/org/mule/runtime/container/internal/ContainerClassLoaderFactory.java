/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.internal;

import static org.mule.runtime.api.util.MuleSystemProperties.classloaderContainerJpmsModuleLayer;

import static java.lang.ClassLoader.getSystemClassLoader;
import static java.util.Objects.requireNonNull;

import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleContainerClassLoaderWrapper;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.jpms.api.MuleContainerModule;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ExportedService;
import org.mule.runtime.module.artifact.api.classloader.FilteringArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Creates the classLoader for the Mule container.
 * <p>
 * This classLoader must be used as the parent classLoader for any other Mule artifact depending only on the container.
 */
public class ContainerClassLoaderFactory {

  private final PreFilteredContainerClassLoaderCreator preFilteredContainerClassLoaderCreator;
  private final Function<ClassLoader, ClassLoader> parentClassLoaderResolver;

  /**
   * Creates a custom factory
   *
   * @param preFilteredContainerClassLoaderCreator encapsulates the logic for creating the parts needed for the container class
   *                                               loader. Non-null.
   * @since 4.5
   */
  public ContainerClassLoaderFactory(PreFilteredContainerClassLoaderCreator preFilteredContainerClassLoaderCreator,
                                     Function<ClassLoader, ClassLoader> parentClassLoaderResolver) {
    requireNonNull(preFilteredContainerClassLoaderCreator, "containerClassLoaderCreator cannot be null");

    this.preFilteredContainerClassLoaderCreator = preFilteredContainerClassLoaderCreator;
    this.parentClassLoaderResolver = parentClassLoaderResolver;
  }

  /**
   * Creates a custom factory
   *
   * @param moduleRepository provides access to the modules available on the container. Non-null.
   */
  public ContainerClassLoaderFactory(ModuleRepository moduleRepository) {
    this(new DefaultPreFilteredContainerClassLoaderCreator(moduleRepository),
         // Keep previous behavior, even if not correct, when using classloaders instead of modules to avoid breaking backwards
         // compatibility accidentally.
         // This is just the criteria to use to toggle the fix, since FeatureFlags are not available at this point.
         classLoader -> classloaderContainerJpmsModuleLayer()
             ? getSystemClassLoader()
             : classLoader);
  }

  /**
   * Creates a custom factory
   *
   * @param moduleRepository                      provides access to the modules available on the container. Non-null.
   * @param bootPackages                          provides a set of packages that define all the prefixes that must be loaded from
   *                                              the container classLoader without being filtered.
   * @param additionalExportedResourceDirectories provides a set of directories of resources that should be additionally exported.
   */
  public ContainerClassLoaderFactory(ModuleRepository moduleRepository, Set<String> bootPackages,
                                     Set<String> additionalExportedResourceDirectories) {
    this(new DefaultPreFilteredContainerClassLoaderCreator(moduleRepository, bootPackages, additionalExportedResourceDirectories),
         // Keep previous behavior, even if not correct, when using classloaders instead of modules to avoid breaking backwards
         // compatibility accidentally.
         // This is just the criteria to use to toggle the fix, since FeatureFlags are not available at this point.
         classLoader -> classloaderContainerJpmsModuleLayer()
             ? getSystemClassLoader()
             : classLoader);
  }

  /**
   * Creates the classLoader to represent the Mule container.
   *
   * @param parentClassLoader parent classLoader. Can be null.
   * @return a non-null {@link ArtifactClassLoader} containing container code that can be used as parent classloader for other
   *         mule artifacts.
   */
  public MuleContainerClassLoaderWrapper createContainerClassLoader(final ClassLoader parentClassLoader) {
    final List<MuleContainerModule> muleModules = preFilteredContainerClassLoaderCreator.getMuleModules();

    return new DefaultMuleContainerClassLoaderWrapper(createArtifactClassLoader(parentClassLoader, muleModules,
                                                                                new ArtifactDescriptor("mule")));
  }

  /**
   * Creates an {@link ArtifactClassLoader} that always resolves resources by delegating to the parentClassLoader.
   *
   * @param parentClassLoader  the parent {@link ClassLoader} for the container
   * @param muleModules        the list of {@link MuleContainerModule}s to be used for defining the filter
   * @param artifactDescriptor descriptor for the artifact owning the created class loader instance.
   * @return a {@link ArtifactClassLoader} to be used in a {@link FilteringContainerClassLoader}
   */
  protected ArtifactClassLoader createArtifactClassLoader(final ClassLoader parentClassLoader,
                                                          List<MuleContainerModule> muleModules,
                                                          ArtifactDescriptor artifactDescriptor) {
    return createContainerFilteringClassLoader(parentClassLoaderResolver.apply(parentClassLoader),
                                               muleModules,
                                               preFilteredContainerClassLoaderCreator
                                                   .getPreFilteredContainerClassLoader(artifactDescriptor, parentClassLoader));
  }

  /**
   * Creates a {@link FilteringArtifactClassLoader} to filter the {@link ArtifactClassLoader} containerClassLoader given based on
   * {@link List<MuleModule>} of muleModules.
   *
   * @param parentClassLoader    the parent {@link ClassLoader} for the container
   * @param muleModules          the list of {@link MuleContainerModule}s to be used for defining the filter
   * @param containerClassLoader the {@link ArtifactClassLoader} for the container that will be used to delegate by the
   *                             {@link FilteringContainerClassLoader}
   * @return a {@link FilteringContainerClassLoader} that would be the one used as the parent of plugins and applications
   *         {@link ArtifactClassLoader}
   */
  protected FilteringArtifactClassLoader createContainerFilteringClassLoader(final ClassLoader parentClassLoader,
                                                                             List<MuleContainerModule> muleModules,
                                                                             ArtifactClassLoader containerClassLoader) {
    return new FilteringContainerClassLoader(parentClassLoader, containerClassLoader,
                                             new ContainerClassLoaderFilterFactory()
                                                 .create(preFilteredContainerClassLoaderCreator.getBootPackages(), muleModules,
                                                         preFilteredContainerClassLoaderCreator
                                                             .getAdditionallyExportedResourceDirectories()),
                                             getExportedServices(muleModules));
  }

  private List<ExportedService> getExportedServices(List<MuleContainerModule> muleModules) {
    List<ExportedService> exportedServices = new ArrayList<>();

    for (MuleContainerModule muleModule : muleModules) {
      if (muleModule instanceof MuleModule) {
        exportedServices.addAll(((MuleModule) muleModule).getExportedServices());
      }
    }

    return exportedServices;
  }

}
