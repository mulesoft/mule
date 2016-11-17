/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.collections.CollectionUtils.find;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.api.util.Preconditions.checkState;
import org.mule.runtime.core.util.UUID;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.artifact.DependenciesProvider;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginRepository;
import org.mule.runtime.deployment.model.internal.plugin.BundlePluginDependenciesResolver;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel;

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
  private final ArtifactClassLoaderFactory<ArtifactPluginDescriptor> artifactPluginClassLoaderFactory;
  private final BundlePluginDependenciesResolver pluginDependenciesResolver;
  private Set<ArtifactPluginDescriptor> artifactPluginDescriptors = new HashSet<>();
  private String artifactId = UUID.getUUID();
  private ArtifactDescriptor artifactDescriptor;
  private ArtifactClassLoader parentClassLoader;
  private List<ArtifactClassLoader> artifactPluginClassLoaders = new ArrayList<>();

  /**
   * Creates an {@link AbstractArtifactClassLoaderBuilder}.
   *  @param artifactClassLoaderFactory factory for the classloader specific to the artifact resource and classes. Must be not
   *        null.
   * @param artifactPluginRepository repository of plugins contained by the runtime. Must be not null.
   * @param artifactPluginClassLoaderFactory factory to create class loaders for each used plugin. Non be not null.
   * @param artifactDescriptorFactory factory to create {@link ArtifactPluginDescriptor} when there's a missing dependency to resolve
   * @param dependenciesProvider resolver for missing dependencies.
   */
  public AbstractArtifactClassLoaderBuilder(DeployableArtifactClassLoaderFactory artifactClassLoaderFactory,
                                            ArtifactPluginRepository artifactPluginRepository,
                                            ArtifactClassLoaderFactory<ArtifactPluginDescriptor> artifactPluginClassLoaderFactory,
                                            ArtifactDescriptorFactory<ArtifactPluginDescriptor> artifactDescriptorFactory,
                                            DependenciesProvider dependenciesProvider) {
    checkArgument(artifactClassLoaderFactory != null, "artifact class loader factory cannot be null");
    checkArgument(artifactPluginRepository != null, "artifact plugin repository cannot be null");
    checkArgument(artifactPluginClassLoaderFactory != null, "artifactPluginClassLoaderFactory cannot be null");
    checkArgument(artifactDescriptorFactory != null, "artifactPluginClassLoaderFactory cannot be null");
    checkArgument(dependenciesProvider != null, "dependenciesProvider cannot be null");
    this.artifactClassLoaderFactory = artifactClassLoaderFactory;
    this.artifactPluginRepository = artifactPluginRepository;
    this.artifactPluginClassLoaderFactory = artifactPluginClassLoaderFactory;
    this.pluginDependenciesResolver = new BundlePluginDependenciesResolver(artifactDescriptorFactory, dependenciesProvider);
  }

  /**
   * Implementation must redefine this method and it should provide the root class loader which is going to be used as parent
   * class loader for every other class loader created by this builder.
   *
   * @return the root class loader for all other class loaders
   */
  protected abstract ArtifactClassLoader getParentClassLoader();

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
    final String artifactId = getArtifactId(artifactDescriptor);
    RegionClassLoader regionClassLoader =
        new RegionClassLoader(artifactId, artifactDescriptor, parentClassLoader.getClassLoader(),
                              parentClassLoader.getClassLoaderLookupPolicy());

    List<ArtifactPluginDescriptor> pluginDescriptors = createContainerApplicationPlugins();
    pluginDescriptors.addAll(artifactPluginDescriptors);
    List<ArtifactPluginDescriptor> effectiveArtifactPluginDescriptors = pluginDependenciesResolver.resolve(pluginDescriptors);

    final List<ArtifactClassLoader> pluginClassLoaders =
        createPluginClassLoaders(artifactId, regionClassLoader, effectiveArtifactPluginDescriptors);

    final ArtifactClassLoader artifactClassLoader =
        artifactClassLoaderFactory.create(artifactId, regionClassLoader, artifactDescriptor, artifactPluginClassLoaders);
    ArtifactClassLoaderFilter artifactClassLoaderFilter = createClassLoaderFilter(artifactDescriptor.getClassLoaderModel());
    regionClassLoader.addClassLoader(artifactClassLoader, artifactClassLoaderFilter);

    for (int i = 0; i < effectiveArtifactPluginDescriptors.size(); i++) {
      final ArtifactClassLoaderFilter classLoaderFilter =
          createClassLoaderFilter(effectiveArtifactPluginDescriptors.get(i).getClassLoaderModel());
      regionClassLoader.addClassLoader(pluginClassLoaders.get(i), classLoaderFilter);
    }

    return artifactClassLoader;
  }

  private ArtifactClassLoaderFilter createClassLoaderFilter(ClassLoaderModel classLoaderModel) {
    return new DefaultArtifactClassLoaderFilter(classLoaderModel.getExportedPackages(), classLoaderModel.getExportedResources());
  }

  protected abstract String getArtifactId(ArtifactDescriptor artifactDescriptor);

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

  private List<ArtifactClassLoader> createPluginClassLoaders(String artifactId, ArtifactClassLoader parent,
                                                             List<ArtifactPluginDescriptor> artifactPluginDescriptors) {
    List<ArtifactClassLoader> classLoaders = new LinkedList<>();

    for (ArtifactPluginDescriptor artifactPluginDescriptor : artifactPluginDescriptors) {
      artifactPluginDescriptor.setArtifactPluginDescriptors(artifactPluginDescriptors);

      final String pluginArtifactId = getArtifactPluginId(artifactId, artifactPluginDescriptor.getName());
      final ArtifactClassLoader artifactClassLoader =
          artifactPluginClassLoaderFactory.create(pluginArtifactId, parent, artifactPluginDescriptor);
      artifactPluginClassLoaders.add(artifactClassLoader);
      classLoaders.add(artifactClassLoader);
    }
    return classLoaders;
  }

  /**
   * @param parentArtifactId identifier of the artifact that owns the plugin. Non empty.
   * @param pluginName name of the plugin. Non empty.
   * @return the unique identifier for the plugin inside the parent artifact.
   */
  public static String getArtifactPluginId(String parentArtifactId, String pluginName) {
    checkArgument(!isEmpty(parentArtifactId), "parentArtifactId cannot be empty");
    checkArgument(!isEmpty(pluginName), "pluginName cannot be empty");

    return parentArtifactId + "/plugin/" + pluginName;
  }
}
