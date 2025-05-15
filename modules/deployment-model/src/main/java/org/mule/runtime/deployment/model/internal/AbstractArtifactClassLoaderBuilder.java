/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;

import static java.util.Arrays.asList;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import org.mule.runtime.deployment.model.api.builder.RegionPluginClassLoadersFactory;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.api.classloader.ChildFirstLookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.DefaultArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all artifacts class loader filters.
 *
 * @param <T> the type of the filer.
 * @since 4.0
 */
public abstract class AbstractArtifactClassLoaderBuilder<T extends AbstractArtifactClassLoaderBuilder> {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final RegionPluginClassLoadersFactory pluginClassLoadersFactory;
  private final List<ArtifactPluginDescriptor> artifactPluginDescriptors = new LinkedList<>();
  protected ArtifactDescriptor artifactDescriptor;

  /**
   * Creates an {@link AbstractArtifactClassLoaderBuilder}.
   *
   * @param pluginClassLoadersFactory creates the class loaders for the plugins included in the artifact's region. Non null
   */
  public AbstractArtifactClassLoaderBuilder(RegionPluginClassLoadersFactory pluginClassLoadersFactory) {
    checkArgument(pluginClassLoadersFactory != null, "pluginClassLoadersFactory cannot be null");
    this.pluginClassLoadersFactory = pluginClassLoadersFactory;
  }

  /**
   * Implementation must redefine this method and it should provide the root class loader which is going to be used as parent
   * class loader for every other class loader created by this builder.
   *
   * @return the root class loader for all other class loaders
   */
  protected abstract ArtifactClassLoader getParentClassLoader();

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
   * @throws RuntimeException exception cause when it was not possible to access the file provided as dependencies
   */
  public ArtifactClassLoader build() {
    checkState(artifactDescriptor != null, "artifact descriptor cannot be null");
    ArtifactClassLoader parentClassLoader = getParentClassLoader();
    checkState(parentClassLoader != null, "parent class loader cannot be null");
    final String artifactId = getArtifactId(artifactDescriptor);
    ClassLoaderLookupPolicy parentLookupPolicy = getParentLookupPolicy(parentClassLoader);
    RegionClassLoader regionClassLoader = createRegionClassLoader(artifactId,
                                                                  artifactDescriptor,
                                                                  parentClassLoader.getClassLoader(),
                                                                  parentLookupPolicy);

    ArtifactClassLoaderFilter artifactClassLoaderFilter =
        createArtifactClassLoaderFilter(artifactDescriptor.getClassLoaderConfiguration(),
                                        parentLookupPolicy);

    Map<String, LookupStrategy> appAdditionalLookupStrategy =
        artifactClassLoaderFilter.getExportedClassPackages().stream()
            .collect(toMap(identity(), p -> PARENT_FIRST));

    final ArtifactClassLoader artifactClassLoader = createArtifactClassLoader(artifactId, regionClassLoader);
    regionClassLoader.addClassLoader(artifactClassLoader, artifactClassLoaderFilter);

    List<ArtifactClassLoader> artifactPluginClassLoaders =
        pluginClassLoadersFactory.createPluginClassLoaders(regionClassLoader, artifactPluginDescriptors,
                                                           regionClassLoader.getClassLoaderLookupPolicy()
                                                               .extend(appAdditionalLookupStrategy));

    int artifactPluginIndex = 0;
    for (ArtifactPluginDescriptor artifactPluginDescriptor : artifactPluginDescriptors) {
      final ArtifactClassLoaderFilter classLoaderFilter =
          createPluginClassLoaderFilter(artifactPluginDescriptor,
                                        artifactDescriptor.getClassLoaderConfiguration().getExportedPackages(),
                                        parentLookupPolicy);
      regionClassLoader.addClassLoader(artifactPluginClassLoaders.get(artifactPluginIndex), classLoaderFilter);
      artifactPluginIndex++;
    }
    return artifactClassLoader;
  }

  /**
   * Template method to build different implementations of a {@link RegionClassLoader}
   *
   * @return a new {@link RegionClassLoader}
   */
  protected RegionClassLoader createRegionClassLoader(String artifactId,
                                                      ArtifactDescriptor artifactDescriptor,
                                                      ClassLoader parentClassLoader,
                                                      ClassLoaderLookupPolicy parentLookupPolicy) {
    return new RegionClassLoader(artifactId,
                                 artifactDescriptor,
                                 parentClassLoader,
                                 parentLookupPolicy);
  }

  /**
   * @param parentClassLoader parent class loader for the creates artifact class loader
   * @return the lookup policy to use on the created artifact class loader
   */
  protected ClassLoaderLookupPolicy getParentLookupPolicy(ArtifactClassLoader parentClassLoader) {
    return parentClassLoader.getClassLoaderLookupPolicy();
  }

  /**
   * Creates the class loader for the artifact being built.
   *
   * @param artifactId        identifies the artifact being created. Non empty.
   * @param regionClassLoader class loader containing the artifact and dependant class loaders. Non null.
   * @return
   */
  protected abstract ArtifactClassLoader createArtifactClassLoader(String artifactId, RegionClassLoader regionClassLoader);

  private ArtifactClassLoaderFilter createArtifactClassLoaderFilter(ClassLoaderConfiguration classLoaderConfiguration,
                                                                    ClassLoaderLookupPolicy classLoaderLookupPolicy) {
    Set<String> artifactExportedPackages = sanitizeExportedPackages(classLoaderLookupPolicy,
                                                                    classLoaderConfiguration.getExportedPackages());

    return new DefaultArtifactClassLoaderFilter(artifactExportedPackages, classLoaderConfiguration.getExportedResources());
  }

  private ArtifactClassLoaderFilter createPluginClassLoaderFilter(ArtifactPluginDescriptor pluginDescriptor,
                                                                  Set<String> parentArtifactExportedPackages,
                                                                  ClassLoaderLookupPolicy classLoaderLookupPolicy) {
    Set<String> sanitizedArtifactExportedPackages =
        sanitizeExportedPackages(classLoaderLookupPolicy, pluginDescriptor.getClassLoaderConfiguration().getExportedPackages());

    Set<String> replacedPackages =
        parentArtifactExportedPackages.stream().filter(p -> sanitizedArtifactExportedPackages.contains(p)).collect(toSet());
    if (!replacedPackages.isEmpty()) {
      sanitizedArtifactExportedPackages.removeAll(replacedPackages);
      logger.warn("Exported packages from plugin '" + pluginDescriptor.getName() + "' are provided by the artifact owner: "
          + replacedPackages);
    }
    return new DefaultArtifactClassLoaderFilter(sanitizedArtifactExportedPackages,
                                                pluginDescriptor.getClassLoaderConfiguration().getExportedResources());
  }

  private Set<String> sanitizeExportedPackages(ClassLoaderLookupPolicy classLoaderLookupPolicy,
                                               Set<String> artifactExportedPackages) {
    Set<String> sanitizedArtifactExportedPackages = new HashSet<>(artifactExportedPackages);

    Set<String> containerProvidedPackages = sanitizedArtifactExportedPackages.stream().filter(p -> {
      LookupStrategy lookupStrategy = classLoaderLookupPolicy.getPackageLookupStrategy(p);
      return !(lookupStrategy instanceof ChildFirstLookupStrategy);
    }).collect(toSet());
    if (!containerProvidedPackages.isEmpty()) {
      sanitizedArtifactExportedPackages.removeAll(containerProvidedPackages);
      logger.warn("Exported packages from artifact '" + artifactDescriptor.getName() + "' are provided by parent class loader: "
          + containerProvidedPackages);
    }
    return sanitizedArtifactExportedPackages;
  }

  protected abstract String getArtifactId(ArtifactDescriptor artifactDescriptor);
}
