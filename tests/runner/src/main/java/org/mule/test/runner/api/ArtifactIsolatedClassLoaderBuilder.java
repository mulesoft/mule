/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static com.google.common.collect.Lists.newArrayList;
import static org.mule.runtime.core.util.Preconditions.checkNotNull;
import org.mule.test.runner.classloader.IsolatedClassLoaderFactory;
import org.mule.test.runner.maven.MavenModelFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * <p/>
 * The object built by this builder is a {@link ArtifactClassLoaderHolder} that references the
 * {@link org.mule.runtime.module.artifact.classloader.ArtifactClassLoader} for the application, plugins and container.
 *
 * @since 4.0
 */
public class ArtifactIsolatedClassLoaderBuilder {

  private static final String POM_XML = "pom.xml";
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private ClassPathClassifier classPathClassifier;
  private ClassPathUrlProvider classPathUrlProvider;

  private IsolatedClassLoaderFactory isolatedClassLoaderFactory = new IsolatedClassLoaderFactory();

  private Artifact rootArtifact;
  private File pluginResourcesFolder;
  private List<String> excludedArtifacts = newArrayList();
  private List<String> providedExclusions = newArrayList();
  private List<String> testExclusions = newArrayList();
  private List<String> testInclusions = newArrayList();
  private List<String> pluginCoordinates = newArrayList();
  private List<Class> exportPluginClasses = newArrayList();
  private boolean extensionMetadataGenerationEnabled = false;
  private List<String> providedInclusions = newArrayList();
  private List<String> extraBootPackages;

  public ArtifactIsolatedClassLoaderBuilder setPluginCoordinates(List<String> pluginCoordinates) {
    this.pluginCoordinates = pluginCoordinates;
    return this;
  }

  /**
   * Sets the {@link ClassPathClassifier} implementation to be used by the builder.
   *
   * @param classPathClassifier {@link ClassPathClassifier} implementation to be used by the builder.
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setClassPathClassifier(final ClassPathClassifier classPathClassifier) {
    this.classPathClassifier = classPathClassifier;
    return this;
  }

  /**
   * Sets the {@link ClassPathUrlProvider} implementation to be used by the builder.
   *
   * @param classPathUrlProvider {@link ClassPathUrlProvider} implementation to be used by the builder.
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setClassPathUrlProvider(final ClassPathUrlProvider classPathUrlProvider) {
    this.classPathUrlProvider = classPathUrlProvider;
    return this;
  }

  /**
   * Sets the {@link File} rootArtifactClassesFolder to be used by the classification process.
   *
   * @param rootArtifactClassesFolder {@link File} rootArtifactClassesFolder to be used by the classification process.
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setRootArtifactClassesFolder(final File rootArtifactClassesFolder) {
    this.rootArtifact = getRootArtifact(rootArtifactClassesFolder);
    return this;
  }

  /**
   * Sets the {@link File} where resources for classification will be created.
   *
   * @param pluginResourcesFolder {@link File} where resources for classification will be created.
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setPluginResourcesFolder(final File pluginResourcesFolder) {
    this.pluginResourcesFolder = pluginResourcesFolder;
    return this;
  }

  /**
   * Sets the Maven artifacts to be excluded from artifact class loaders created here due to they are going to be added as boot
   * packages. In format {@code <groupId>:<artifactId>:[[<extension>]:<version>]}.
   *
   * @param excludedArtifacts Maven artifacts to be excluded from artifact class loaders created here due to they are going to be
   *        added as boot packages. In format {@code <groupId>:<artifactId>:[[<extension>]:<version>]}.
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setExcludedArtifacts(List<String> excludedArtifacts) {
    this.excludedArtifacts = excludedArtifacts;
    return this;
  }

  /**
   * Sets the {@link List} of {@link String}s containing the extra boot packages defined to be appended to the container in
   * addition to the pre-defined ones.
   * 
   * @param extraBootPackages {@link List} of {@link String}s containing the extra boot packages defined to be appended to the
   *        container in addition to the pre-defined ones.
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setExtraBootPackages(List<String> extraBootPackages) {
    this.extraBootPackages = extraBootPackages;
    return this;
  }

  /**
   * Sets Maven artifacts to be excluded from the {@code provided} scope direct dependencies of the rootArtifact. In format
   * {@code <groupId>:<artifactId>:<extension>:<version>}.
   * <p/>
   * {@link #setPluginCoordinates(List)} Maven artifacts if declared will be considered to be excluded from being added as
   * {@code provided} due to they are going to be added to its class loaders.
   *
   * @param providedExclusions Maven artifacts to be excluded from the {@code provided} scope direct dependencies of the
   *        rootArtifact. In format {@code <groupId>:<artifactId>:[[<extension>]:<version>]}.
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setProvidedExclusions(final List<String> providedExclusions) {
    this.providedExclusions = providedExclusions;
    return this;
  }

  /**
   * Sets Maven artifacts to be explicitly included from the {@code provided} scope direct dependencies of the rootArtifact. In
   * format {@code <groupId>:<artifactId>:[[<classifier>]:<version>]}.
   * <p/>
   * This artifacts have to be declared as {@code provided} scope in rootArtifact direct dependencies and no matter if they were
   * excluded or not from {@link #setProvidedExclusions(List)} and {@link #setPluginCoordinates(List)}. Meaning that the same
   * artifact could ended up being added to the container class loader and as plugin.
   *
   * @param providedInclusions
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setProvidedInclusions(List<String> providedInclusions) {
    this.providedInclusions = providedInclusions;
    return this;
  }

  /**
   * Sets the {@link List} of exclusion Maven coordinates to be excluded from test dependencies of rootArtifact. In format
   * {@code <groupId>:<artifactId>:[[<extension>]:<version>]}.
   *
   * @param testExclusions {@link List} of exclusion Maven coordinates to be excluded from test dependencies of rootArtifact. In
   *        format {@code <groupId>:<artifactId>:[[<extension>]:<version>]}.
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setTestExclusions(final List<String> testExclusions) {
    this.testExclusions = testExclusions;
    return this;
  }

  /**
   * Sets the {@link List} of inclusion Maven coordinates to be included from test dependencies of rootArtifact. In format
   * {@code <groupId>:<artifactId>:[[<classifier>]:<version>]}.
   *
   * @param testInclusions {@link List} of inclusion Maven coordinates to be excluded from test dependencies of rootArtifact. In
   *        format {@code <groupId>:<artifactId>:[[<classifier>]:<version>]}.
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setTestInclusions(final List<String> testInclusions) {
    this.testInclusions = testInclusions;
    return this;
  }

  /**
   * Sets the {@link List} of {@link Class}es to be exported by rootArtifact (if it is a Mule plugin) in addition to their APIs,
   * for testing purposes only.
   *
   * @param exportPluginClasses of {@link Class}es to be exported by rootArtifact (if it is a Mule plugin) in addition to their APIs,
   *                            for testing purposes only.
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setExportPluginClasses(final List<Class> exportPluginClasses) {
    this.exportPluginClasses = exportPluginClasses;
    return this;
  }

  /**
   * Sets to {@code true} if while building the a plugin {@link org.mule.runtime.module.artifact.classloader.ArtifactClassLoader}
   * for an {@link org.mule.runtime.extension.api.annotation.Extension} the metadata should be generated.
   *
   * @param extensionMetadataGenerationEnabled {@code boolean} to enable Extension metadata generation.
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setExtensionMetadataGeneration(final boolean extensionMetadataGenerationEnabled) {
    this.extensionMetadataGenerationEnabled = extensionMetadataGenerationEnabled;
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
    checkNotNull(rootArtifact, "rootArtifact has to be set");
    checkNotNull(classPathUrlProvider, "classPathUrlProvider has to be set");
    checkNotNull(classPathClassifier, "classPathClassifier has to be set");

    ClassPathClassifierContext context;
    try {
      context =
          new ClassPathClassifierContext(rootArtifact,
                                         pluginResourcesFolder,
                                         classPathUrlProvider.getURLs(),
                                         excludedArtifacts,
                                         extraBootPackages,
                                         providedExclusions,
                                         providedInclusions,
                                         testExclusions,
                                         testInclusions,
                                         pluginCoordinates, exportPluginClasses, extensionMetadataGenerationEnabled);
    } catch (IOException e) {
      throw new RuntimeException("Error while creating the classification context", e);
    }

    ArtifactUrlClassification artifactUrlClassification = classPathClassifier.classify(context);
    return isolatedClassLoaderFactory.createArtifactClassLoader(context.getExtraBootPackages(), artifactUrlClassification);
  }

  /**
   * Gets the Maven artifact located at the given by reading the {@value #POM_XML} file two levels up from target/classes.
   *
   * @param rootArtifactClassesFolder {@link File} the rootArtifactClassesFolder
   * @return {@link Artifact} that represents the rootArtifact
   */
  private Artifact getRootArtifact(File rootArtifactClassesFolder) {
    File pomFile = new File(rootArtifactClassesFolder.getParentFile().getParentFile(), POM_XML);
    logger.debug("Reading rootArtifact from pom file: {}", pomFile);
    Model model = MavenModelFactory.createMavenProject(pomFile);

    return new DefaultArtifact(model.getGroupId() != null ? model.getGroupId() : model.getParent().getGroupId(),
                               model.getArtifactId(), model.getPackaging(),
                               model.getVersion() != null ? model.getVersion() : model.getParent().getVersion());
  }

}
