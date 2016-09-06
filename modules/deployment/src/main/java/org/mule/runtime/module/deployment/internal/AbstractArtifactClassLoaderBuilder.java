/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.collections.CollectionUtils.find;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.core.util.Preconditions.checkState;
import org.mule.runtime.core.util.UUID;
import org.mule.runtime.module.deployment.api.DeploymentException;
import org.mule.runtime.module.deployment.internal.application.ArtifactPlugin;
import org.mule.runtime.module.deployment.internal.application.ArtifactPluginFactory;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.FilteringArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.deployment.internal.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.deployment.internal.plugin.ArtifactPluginRepository;
import org.mule.runtime.module.deployment.internal.plugin.NamePluginDependenciesResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Base class for all artifacts class loader filters.
 *
 * @param <T> the type of the filer.
 * @since 4.0
 */
public abstract class AbstractArtifactClassLoaderBuilder<T extends AbstractArtifactClassLoaderBuilder> {

  private final DeployableArtifactClassLoaderFactory artifactClassLoaderFactory;
  private final ArtifactPluginRepository artifactPluginRepository;
  private final ArtifactPluginFactory artifactPluginFactory;
  private Set<ArtifactPluginDescriptor> artifactPluginDescriptors = new HashSet<>();
  private String artifactId = UUID.getUUID();
  private ArtifactDescriptor artifactDescriptor;
  private ArtifactClassLoader parentClassLoader;
  private List<ArtifactClassLoader> artifactPluginClassLoaders = new ArrayList<>();

  /**
   * Creates an {@link AbstractArtifactClassLoaderBuilder}.
   *
   * @param artifactClassLoaderFactory factory for the classloader specific to the artifact resource and classes. Must be not
   *        null.
   * @param artifactPluginRepository repository of plugins contained by the runtime. Must be not null.
   * @param artifactPluginFactory factory for creating artifact plugins. Must be not null.
   */
  public AbstractArtifactClassLoaderBuilder(DeployableArtifactClassLoaderFactory artifactClassLoaderFactory,
                                            ArtifactPluginRepository artifactPluginRepository,
                                            ArtifactPluginFactory artifactPluginFactory) {
    checkArgument(artifactClassLoaderFactory != null, "artifact class loader factory cannot be null");
    checkArgument(artifactPluginRepository != null, "artifact plugin repository cannot be null");
    checkArgument(artifactPluginFactory != null, "artifact plugin factory cannot be null");
    this.artifactClassLoaderFactory = artifactClassLoaderFactory;
    this.artifactPluginRepository = artifactPluginRepository;
    this.artifactPluginFactory = artifactPluginFactory;
  }

  /**
   * Implementation must redefine this method and it should provide the root class loader which is going to be used as parent
   * class loader for every other class loader created by this builder.
   *
   * @return the root class loader for all other class loaders
   */
  abstract ArtifactClassLoader getParentClassLoader();

  /**
   * @param artifactId unique identifier for this artifact. For instance, for Applications, it can be the app name. Must be not
   *        null.
   * @return the builder
   */
  public T setArtifactId(String artifactId) {
    checkArgument(artifactId != null, "artifact id cannot be null");
    this.artifactId = artifactId;
    return (T) this;
  }

  /**
   * @param artifactPluginDescriptors set of plugins descriptors that will be used by the application.
   * @return the builder
   */
  public T addArtifactPluginDescriptors(ArtifactPluginDescriptor... artifactPluginDescriptors) {
    checkArgument(artifactPluginDescriptors != null, "artifact plugin descriptors cannot be null");
    this.artifactPluginDescriptors.addAll(asList(artifactPluginDescriptors));
    return (T) this;
  }

  /**
   * @param artifactDescriptor the descriptor of the artifact for which the class loader is going to be created.
   * @return the builder
   */
  public T setArtifactDescriptor(ArtifactDescriptor artifactDescriptor) {
    this.artifactDescriptor = artifactDescriptor;
    return (T) this;
  }

  /**
   * Creates a new {@code ArtifactClassLoader} using the provided configuration. It will create the proper class loader hierarchy
   * and filters the artifact resources and plugins classes and resources are resolve correctly.
   *
   * @return a {@code ArtifactClassLoader} created from the provided configuration.
   * @throws IOException exception cause when it was not possible to access the file provided as dependencies
   */
  public ArtifactClassLoader build() throws IOException {
    checkState(artifactDescriptor != null, "artifact descriptor cannot be null");
    parentClassLoader = getParentClassLoader();
    checkState(parentClassLoader != null, "parent class loader cannot be null");
    RegionClassLoader regionClassLoader = new RegionClassLoader("Region" + artifactId, parentClassLoader.getClassLoader(),
                                                                parentClassLoader.getClassLoaderLookupPolicy());

    List<ArtifactPluginDescriptor> effectiveArtifactPluginDescriptors = createContainerApplicationPlugins();
    effectiveArtifactPluginDescriptors.addAll(artifactPluginDescriptors);
    effectiveArtifactPluginDescriptors = new NamePluginDependenciesResolver().resolve(effectiveArtifactPluginDescriptors);

    final List<ArtifactClassLoader> pluginClassLoaders =
        createPluginClassLoaders(regionClassLoader, effectiveArtifactPluginDescriptors);

    final ArtifactClassLoader artifactClassLoader =
        artifactClassLoaderFactory.create(regionClassLoader, artifactDescriptor, artifactPluginClassLoaders);
    regionClassLoader.addClassLoader(artifactClassLoader, artifactDescriptor.getClassLoaderFilter());

    for (int i = 0; i < effectiveArtifactPluginDescriptors.size(); i++) {
      final ArtifactClassLoaderFilter classLoaderFilter = effectiveArtifactPluginDescriptors.get(i).getClassLoaderFilter();
      regionClassLoader.addClassLoader(pluginClassLoaders.get(i), classLoaderFilter);
    }
    return artifactClassLoader;
  }

  private List<ArtifactPluginDescriptor> createContainerApplicationPlugins() {
    final List<ArtifactPluginDescriptor> containerPlugins = new LinkedList<>();
    for (ArtifactPluginDescriptor appPluginDescriptor : artifactPluginRepository.getContainerArtifactPluginDescriptors()) {
      if (containsApplicationPluginDescriptor(appPluginDescriptor)) {
        final String msg =
            format("Failed to deploy artifact [%s], plugin [%s] is already bundled within the container and cannot be included in artifact",
                   artifactId, appPluginDescriptor.getName());
        throw new DeploymentException(createStaticMessage(msg));
      }

      containerPlugins.add(appPluginDescriptor);
    }
    return containerPlugins;
  }

  /**
   * @param appPluginDescriptor
   * @return true if this application has the given appPluginDescriptor already defined in its artifactPluginDescriptors list.
   */
  private boolean containsApplicationPluginDescriptor(ArtifactPluginDescriptor appPluginDescriptor) {
    return find(this.artifactPluginDescriptors,
                object -> ((ArtifactPluginDescriptor) object).getName().equals(appPluginDescriptor.getName())) != null;
  }

  private List<ArtifactClassLoader> createPluginClassLoaders(ArtifactClassLoader parent,
                                                             List<ArtifactPluginDescriptor> artifactPluginDescriptors) {
    List<ArtifactClassLoader> classLoaders = new LinkedList<>();

    for (ArtifactPluginDescriptor artifactPluginDescriptor : artifactPluginDescriptors) {
      artifactPluginDescriptor.setArtifactPluginDescriptors(artifactPluginDescriptors);

      ArtifactPlugin artifactPlugin = artifactPluginFactory.create(artifactPluginDescriptor, parent);
      artifactPluginClassLoaders.add(artifactPlugin.getArtifactClassLoader());

      final FilteringArtifactClassLoader filteringPluginClassLoader =
          new FilteringArtifactClassLoader(artifactPlugin.getArtifactClassLoader(),
                                           artifactPlugin.getDescriptor().getClassLoaderFilter());
      classLoaders.add(filteringPluginClassLoader);
    }
    return classLoaders;
  }
}
