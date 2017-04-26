/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.module.artifact.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import org.mule.runtime.core.util.UUID;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.ChildFirstLookupStrategy;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.LookupStrategy;
import org.mule.runtime.module.artifact.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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

  public static final String PLUGIN_CLASSLOADER_IDENTIFIER = "/plugin/";

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());
  protected final ArtifactClassLoaderFactory artifactPluginClassLoaderFactory;
  private Set<ArtifactPluginDescriptor> artifactPluginDescriptors = new HashSet<>();
  private String artifactId = UUID.getUUID();
  protected ArtifactDescriptor artifactDescriptor;
  private ArtifactClassLoader parentClassLoader;
  protected List<ArtifactClassLoader> artifactPluginClassLoaders = new ArrayList<>();

  /**
   * Creates an {@link AbstractArtifactClassLoaderBuilder}.
   *
   * @param artifactPluginClassLoaderFactory factory to create class loaders for each used plugin. Non be not null.
   */
  public AbstractArtifactClassLoaderBuilder(ArtifactClassLoaderFactory<ArtifactPluginDescriptor> artifactPluginClassLoaderFactory) {
    checkArgument(artifactPluginClassLoaderFactory != null, "artifactPluginClassLoaderFactory cannot be null");

    this.artifactPluginClassLoaderFactory = artifactPluginClassLoaderFactory;
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

    ArtifactClassLoaderFilter artifactClassLoaderFilter =
        createArtifactClassLoaderFilter(artifactDescriptor.getClassLoaderModel(),
                                        parentClassLoader.getClassLoaderLookupPolicy());

    Map<String, LookupStrategy> appAdditionalLookupStrategy = new HashMap<>();
    artifactClassLoaderFilter.getExportedClassPackages().stream().forEach(p -> appAdditionalLookupStrategy.put(p, PARENT_FIRST));

    final List<ArtifactClassLoader> pluginClassLoaders =
        createPluginClassLoaders(artifactId, regionClassLoader, artifactPluginDescriptors,
                                 regionClassLoader.getClassLoaderLookupPolicy().extend(appAdditionalLookupStrategy));

    final ArtifactClassLoader artifactClassLoader = createArtifactClassLoader(artifactId, regionClassLoader);

    regionClassLoader.addClassLoader(artifactClassLoader, artifactClassLoaderFilter);

    int artifactPluginIndex = 0;
    for (ArtifactPluginDescriptor artifactPluginDescriptor : artifactPluginDescriptors) {
      final ArtifactClassLoaderFilter classLoaderFilter =
          createPluginClassLoaderFilter(artifactPluginDescriptor, artifactDescriptor.getClassLoaderModel().getExportedPackages(),
                                        parentClassLoader.getClassLoaderLookupPolicy());
      regionClassLoader.addClassLoader(pluginClassLoaders.get(artifactPluginIndex), classLoaderFilter);
      artifactPluginIndex++;
    }
    return artifactClassLoader;
  }

  /**
   * Creates the class loader for the artifact being built.
   *
   * @param artifactId identifies the artifact being created. Non empty.
   * @param regionClassLoader class loader containing the artifact and dependant class loaders. Non null.
   * @return
   */
  protected abstract ArtifactClassLoader createArtifactClassLoader(String artifactId, RegionClassLoader regionClassLoader);

  private ArtifactClassLoaderFilter createArtifactClassLoaderFilter(ClassLoaderModel classLoaderModel,
                                                                    ClassLoaderLookupPolicy classLoaderLookupPolicy) {
    Set<String> artifactExportedPackages = sanitizeExportedPackages(classLoaderLookupPolicy,
                                                                    classLoaderModel.getExportedPackages());

    return new DefaultArtifactClassLoaderFilter(artifactExportedPackages, classLoaderModel.getExportedResources());
  }

  private ArtifactClassLoaderFilter createPluginClassLoaderFilter(ArtifactPluginDescriptor pluginDescriptor,
                                                                  Set<String> parentArtifactExportedPackages,
                                                                  ClassLoaderLookupPolicy classLoaderLookupPolicy) {
    Set<String> sanitizedArtifactExportedPackages =
        sanitizeExportedPackages(classLoaderLookupPolicy, pluginDescriptor.getClassLoaderModel().getExportedPackages());

    Set<String> replacedPackages =
        parentArtifactExportedPackages.stream().filter(p -> sanitizedArtifactExportedPackages.contains(p)).collect(toSet());
    if (!replacedPackages.isEmpty()) {
      sanitizedArtifactExportedPackages.removeAll(replacedPackages);
      logger.warn("Exported packages from plugin '" + pluginDescriptor.getName() + "' are provided by the artifact owner: "
          + replacedPackages);
    }
    return new DefaultArtifactClassLoaderFilter(sanitizedArtifactExportedPackages,
                                                pluginDescriptor.getClassLoaderModel().getExportedResources());
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

  private List<ArtifactClassLoader> createPluginClassLoaders(String artifactId, ArtifactClassLoader parent,
                                                             Set<ArtifactPluginDescriptor> artifactPluginDescriptors,
                                                             ClassLoaderLookupPolicy appExportedPackagesLookupPolicy) {
    List<ArtifactClassLoader> classLoaders = new LinkedList<>();

    for (ArtifactPluginDescriptor artifactPluginDescriptor : artifactPluginDescriptors) {
      artifactPluginDescriptor.setArtifactPluginDescriptors(artifactPluginDescriptors);

      final String pluginArtifactId = getArtifactPluginId(artifactId, artifactPluginDescriptor.getName());

      final ArtifactClassLoader artifactClassLoader =
          artifactPluginClassLoaderFactory.create(pluginArtifactId, artifactPluginDescriptor, parent.getClassLoader(),
                                                  appExportedPackagesLookupPolicy);
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

    return parentArtifactId + PLUGIN_CLASSLOADER_IDENTIFIER + pluginName;
  }

}
