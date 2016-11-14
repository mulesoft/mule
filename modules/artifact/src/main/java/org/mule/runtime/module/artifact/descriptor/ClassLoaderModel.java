/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.descriptor;

import static java.util.Arrays.asList;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contains all the information needed to create a {@link ClassLoader} for a Mule artifact.
 */
public class ClassLoaderModel {

  /**
   * Defines a {@link ClassLoaderModel} with empty configuration
   */
  public static final ClassLoaderModel NULL_CLASSLOADER_MODEL =
      new ClassLoaderModel(new URL[0], new HashSet<>(), new HashSet<>(), new HashSet<>());

  private final URL[] urls;
  private final Set<String> exportedPackages;
  private final Set<String> exportedResources;
  private final Set<BundleDependency> dependencies;

  private ClassLoaderModel(URL[] urls, Set<String> exportedPackages, Set<String> exportedResources,
                           Set<BundleDependency> dependencies) {
    this.urls = urls;
    this.exportedPackages = exportedPackages;
    this.exportedResources = exportedResources;
    this.dependencies = dependencies;
  }

  /**
   * @return the URLs to use on the {@link ClassLoader}. Non null
   */
  public URL[] getUrls() {
    return urls;
  }

  /**
   * @return the class packages to be exported on the {@link ClassLoader}. Non null
   */
  public Set<String> getExportedPackages() {
    return exportedPackages;
  }

  /**
   * @return the resource packages to be exported on the {@link ClassLoader}. Non null
   */
  public Set<String> getExportedResources() {
    return exportedResources;
  }

  /**
   * @return the artifact dependencies required to create the {@link ClassLoader}. Non null
   */
  public Set<BundleDependency> getDependencies() {
    return dependencies;
  }

  /**
   * Builds a {@link ClassLoaderModel}
   */
  public static class ClassLoaderModelBuilder {

    private Set<String> packages = new HashSet<>();
    private Set<String> resources = new HashSet<>();
    private List<URL> urls = new ArrayList<>();
    private Set<BundleDependency> dependencies = new HashSet<>();

    /**
     * Creates an empty builder.
     */
    public ClassLoaderModelBuilder() {}

    /**
     * Creates a builder initialized with a {@link ClassLoaderModel}'s state
     *
     * @param source used to initialize the created object. Non null.
     */
    public ClassLoaderModelBuilder(ClassLoaderModel source) {
      checkArgument(source != null, "source cannot be null");

      this.packages.addAll(source.exportedPackages);
      this.resources.addAll(source.exportedResources);
      this.urls = new ArrayList<>(asList(source.urls));
      this.dependencies.addAll(source.dependencies);
    }

    /**
     * Indicates which package are exported on the model.
     *
     * @param packages packages to export. No null.
     * @return same builder instance.
     */
    public ClassLoaderModelBuilder exportingPackages(Set<String> packages) {
      checkArgument(packages != null, "packages cannot be null");
      this.packages = packages;
      return this;
    }

    /**
     * Indicates which resource are exported on the model.
     *
     * @param resources resources to export. No null.
     * @return same builder instance.
     */
    public ClassLoaderModelBuilder exportingResources(Set<String> resources) {
      checkArgument(resources != null, "resources cannot be null");
      this.resources = resources;
      return this;
    }

    /**
     * Indicates which dependencies are required for this model.
     *
     * @param dependencies dependencies on which the model depends on. Non null.
     * @return same builder instance.
     */
    public ClassLoaderModelBuilder dependingOn(Set<BundleDependency> dependencies) {
      checkArgument(dependencies != null, "dependencies cannot be null");
      this.dependencies = dependencies;
      return this;
    }

    /**
     * Adds an {@link URL} to the model
     *
     * @param url indicates which resource to add. Non null.
     * @return same builder instance.
     */
    public ClassLoaderModelBuilder containing(URL url) {
      checkArgument(url != null, "url cannot be null");
      urls.add(url);
      return this;
    }

    /**
     * Creates a {@link ClassLoaderModel} with the current configuration.
     * @return a non null {@link ClassLoaderModel}
     */
    public ClassLoaderModel build() {
      return new ClassLoaderModel(urls.toArray(new URL[0]), packages, resources, dependencies);
    }
  }
}
