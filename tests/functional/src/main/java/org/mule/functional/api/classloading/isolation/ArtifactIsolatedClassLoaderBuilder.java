/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.api.classloading.isolation;

import static org.mule.runtime.core.util.Preconditions.checkNotNull;
import org.mule.functional.classloading.isolation.classification.DefaultClassPathClassifier;
import org.mule.functional.classloading.isolation.classloader.IsolatedClassLoaderFactory;
import org.mule.functional.classloading.isolation.maven.DefaultMavenMultiModuleArtifactMapping;
import org.mule.functional.classloading.isolation.maven.DependencyGraphMavenDependenciesResolver;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Builds a class loading model that mimics the class loading model used in a standalone container. Useful for running
 * applications or tests in a lightweight environment with isolation.
 * <p/>
 * The builder could be set with different extension points:
 * <ul>
 * <li>{@link ClassPathUrlProvider}: defines the initial classpath to be classified, it consists in a {@link List} of
 * {@link java.net.URL}'s</li>
 * <li>{@link ClassPathClassifier}: classifies the classpath URLs and builds the {@link List} or {@link java.net.URL}s for each
 * {@link ClassLoader}</li>
 * <li>{@link MavenDependenciesResolver}: builds the dependency graph used by the classification in order to get the dependencies
 * for the different Maven artifacts</li>
 * <li>{@link MavenMultiModuleArtifactMapping}: maps an artifactId with a local folder in a multi-module Maven project style</li>
 * </ul>
 * For each of these extension points there is a default implementation already provided with this API.
 * <p/>
 * The object built by this builder is a {@link ArtifactClassLoaderHolder} that references the
 * {@link org.mule.runtime.module.artifact.classloader.ArtifactClassLoader} for the application, plugins and container.
 *
 * @since 4.0
 */
public class ArtifactIsolatedClassLoaderBuilder {

  private ClassPathClassifier classPathClassifier = new DefaultClassPathClassifier();
  private MavenDependenciesResolver mavenDependenciesResolver = new DependencyGraphMavenDependenciesResolver();
  private ClassPathUrlProvider classPathUrlProvider = new ClassPathUrlProvider();
  private MavenMultiModuleArtifactMapping mavenMultiModuleArtifactMapping = new DefaultMavenMultiModuleArtifactMapping();

  private IsolatedClassLoaderFactory isolatedClassLoaderFactory = new IsolatedClassLoaderFactory();

  private File rootArtifactClassesFolder;
  private File rootArtifactTestClassesFolder;
  private List<String> exclusions;
  private List<String> extraBootPackages;
  private List<String> extensionBasePackages;
  private Set<Class> exportClasses;

  /**
   * Sets the {@link ClassPathClassifier} implementation to be used by the builder.
   *
   * @param classPathClassifier
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setClassPathClassifier(final ClassPathClassifier classPathClassifier) {
    this.classPathClassifier = classPathClassifier;
    return this;
  }

  /**
   * Sets the {@link MavenDependenciesResolver} implementation to be used by the builder.
   *
   * @param mavenDependenciesResolver
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setMavenDependenciesResolver(final MavenDependenciesResolver mavenDependenciesResolver) {
    this.mavenDependenciesResolver = mavenDependenciesResolver;
    return this;
  }

  /**
   * Sets the {@link ClassPathUrlProvider} implementation to be used by the builder.
   *
   * @param classPathUrlProvider
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setClassPathUrlProvider(final ClassPathUrlProvider classPathUrlProvider) {
    this.classPathUrlProvider = classPathUrlProvider;
    return this;
  }

  /**
   * Sets the {@link MavenMultiModuleArtifactMapping} implementation to be used by the builder.
   *
   * @param mavenMultiModuleArtifactMapping
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setMavenMultiModuleArtifactMapping(final MavenMultiModuleArtifactMapping mavenMultiModuleArtifactMapping) {
    this.mavenMultiModuleArtifactMapping = mavenMultiModuleArtifactMapping;
    return this;
  }

  /**
   * Sets the {@link File} rootArtifactClassesFolder to be used by the classification process.
   *
   * @param rootArtifactClassesFolder
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setRootArtifactClassesFolder(final File rootArtifactClassesFolder) {
    this.rootArtifactClassesFolder = rootArtifactClassesFolder;
    return this;
  }

  /**
   * Sets the {@link File} rootArtifactTestClassesFolder to be used by the classification process.
   *
   * @param rootArtifactTestClassesFolder
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setRootArtifactTestClassesFolder(final File rootArtifactTestClassesFolder) {
    this.rootArtifactTestClassesFolder = rootArtifactTestClassesFolder;
    return this;
  }

  /**
   * Sets the {@link List} of exclusion packages to be used by the classification process.
   *
   * @param exclusions
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setExclusions(final List<String> exclusions) {
    this.exclusions = exclusions;
    return this;
  }

  /**
   * Sets the {@link List} of extra boot packages to be used by the classification process.
   *
   * @param extraBootPackages
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setExtraBootPackages(final List<String> extraBootPackages) {
    this.extraBootPackages = extraBootPackages;
    return this;
  }

  /**
   * Sets the {@link List} of extensions base packages to be used by the classification process for discovering extensions.
   *
   * @param extensionBasePackages
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setExtensionBasePackages(final List<String> extensionBasePackages) {
    this.extensionBasePackages = extensionBasePackages;
    return this;
  }

  /**
   * Sets the {@link List} of {@link Class}es to be exported by plugins in addition to their APIs, for testing purposes only.
   *
   * @param exportClasses
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setExportClasses(final Set<Class> exportClasses) {
    this.exportClasses = exportClasses;
    return this;
  }

  /**
   * Builds the {@link ArtifactClassLoaderHolder} with the
   * {@link org.mule.runtime.module.artifact.classloader.ArtifactClassLoader}s for application, plugins and container.
   *
   * @return a {@link ArtifactClassLoaderHolder} as output of the classification process.
   * @throws {@link IOException} if there was an error while creating the classification context
   * @throws {@link NullPointerException} if any of the required attributes is not set to this builder
   */
  public ArtifactClassLoaderHolder build() {
    checkNotNull(rootArtifactClassesFolder, "rootArtifactClassesFolder has to be set");
    checkNotNull(rootArtifactTestClassesFolder, "rootArtifactTestClassesFolder has to be set");
    checkNotNull(classPathUrlProvider, "classPathUrlProvider has to be set");
    checkNotNull(mavenDependenciesResolver, "mavenDependenciesResolver has to be set");
    checkNotNull(mavenMultiModuleArtifactMapping, "mavenMultiModuleArtifactMapping has to be set");

    ClassPathClassifierContext context;
    try {
      context =
          new ClassPathClassifierContext(rootArtifactClassesFolder, rootArtifactTestClassesFolder, classPathUrlProvider.getURLs(),
                                         mavenDependenciesResolver.buildDependencies(), mavenMultiModuleArtifactMapping,
                                         exclusions, extraBootPackages, extensionBasePackages, exportClasses);
    } catch (IOException e) {
      throw new RuntimeException("Error while creating the classification context", e);
    }

    ArtifactUrlClassification artifactUrlClassification = classPathClassifier.classify(context);
    return isolatedClassLoaderFactory.createArtifactClassLoader(context.getExtraBootPackages(), artifactUrlClassification);
  }
}
