/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.container.internal;

import static org.mule.runtime.api.util.MuleSystemProperties.classloaderContainerJpmsModuleLayer;

import static java.lang.ClassLoader.getSystemClassLoader;
import static java.util.Objects.requireNonNull;

import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleContainerClassLoaderWrapper;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ExportedService;
import org.mule.runtime.module.artifact.api.classloader.FilteringArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates the classLoader for the Mule container.
 * <p>
 * This classLoader must be used as the parent classLoader for any other Mule artifact depending only on the container.
 */
public class ContainerClassLoaderFactory {

  private final PreFilteredContainerClassLoaderCreator preFilteredContainerClassLoaderCreator;

  /**
   * Creates a custom factory
   *
   * @param preFilteredContainerClassLoaderCreator encapsulates the logic for creating the parts needed for the container class
   *                                               loader. Non-null.
   * @since 4.5
   */
  public ContainerClassLoaderFactory(PreFilteredContainerClassLoaderCreator preFilteredContainerClassLoaderCreator) {
    requireNonNull(preFilteredContainerClassLoaderCreator, "containerClassLoaderCreator cannot be null");

    this.preFilteredContainerClassLoaderCreator = preFilteredContainerClassLoaderCreator;
  }

  /**
   * Creates a custom factory
   *
   * @param moduleRepository provides access to the modules available on the container. Non-null.
   */
  public ContainerClassLoaderFactory(ModuleRepository moduleRepository) {
    this(new DefaultPreFilteredContainerClassLoaderCreator(moduleRepository));
  }

  /**
   * Creates a default factory
   */
  public ContainerClassLoaderFactory() {
    this(new DefaultModuleRepository(new ContainerModuleDiscoverer(ContainerClassLoaderFactory.class.getClassLoader())));
  }

  /**
   * Creates the classLoader to represent the Mule container.
   *
   * @param parentClassLoader parent classLoader. Can be null.
   * @return a non-null {@link ArtifactClassLoader} containing container code that can be used as parent classloader for other
   *         mule artifacts.
   */
  public MuleContainerClassLoaderWrapper createContainerClassLoader(final ClassLoader parentClassLoader) {
    final List<MuleModule> muleModules = preFilteredContainerClassLoaderCreator.getMuleModules();

    return new DefaultMuleContainerClassLoaderWrapper(createArtifactClassLoader(parentClassLoader, muleModules,
                                                                                new ArtifactDescriptor("mule")));
  }

  /**
   * Creates an {@link ArtifactClassLoader} that always resolves resources by delegating to the parentClassLoader.
   *
   * @param parentClassLoader  the parent {@link ClassLoader} for the container
   * @param muleModules        the list of {@link MuleModule}s to be used for defining the filter
   * @param artifactDescriptor descriptor for the artifact owning the created class loader instance.
   * @return a {@link ArtifactClassLoader} to be used in a {@link FilteringContainerClassLoader}
   */
  protected ArtifactClassLoader createArtifactClassLoader(final ClassLoader parentClassLoader, List<MuleModule> muleModules,
                                                          ArtifactDescriptor artifactDescriptor) {
    return createContainerFilteringClassLoader(classloaderContainerJpmsModuleLayer()
        ? getSystemClassLoader()
        // Keep previous behavior, even if not correct, to avoid breaking backwards compatibility accidentally
        : parentClassLoader,
                                               muleModules,
                                               preFilteredContainerClassLoaderCreator
                                                   .getPreFilteredContainerClassLoader(artifactDescriptor, parentClassLoader));
  }

  /**
   * Creates a {@link FilteringArtifactClassLoader} to filter the {@link ArtifactClassLoader} containerClassLoader given based on
   * {@link List<MuleModule>} of muleModules.
   *
   * @param parentClassLoader    the parent {@link ClassLoader} for the container
   * @param muleModules          the list of {@link MuleModule}s to be used for defining the filter
   * @param containerClassLoader the {@link ArtifactClassLoader} for the container that will be used to delegate by the
   *                             {@link FilteringContainerClassLoader}
   * @return a {@link FilteringContainerClassLoader} that would be the one used as the parent of plugins and applications
   *         {@link ArtifactClassLoader}
   */
  protected FilteringArtifactClassLoader createContainerFilteringClassLoader(final ClassLoader parentClassLoader,
                                                                             List<MuleModule> muleModules,
                                                                             ArtifactClassLoader containerClassLoader) {
    return new FilteringContainerClassLoader(parentClassLoader, containerClassLoader,
                                             new ContainerClassLoaderFilterFactory()
                                                 .create(preFilteredContainerClassLoaderCreator.getBootPackages(), muleModules),
                                             getExportedServices(muleModules));
  }

  private List<ExportedService> getExportedServices(List<MuleModule> muleModules) {
    List<ExportedService> exportedServices = new ArrayList<>();

    for (MuleModule muleModule : muleModules) {
      exportedServices.addAll(muleModule.getExportedServices());
    }

    return exportedServices;
  }

}
