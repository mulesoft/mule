/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptySet;
import static org.mule.runtime.api.util.Preconditions.checkNotNull;

import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.test.runner.classloader.IsolatedClassLoaderFactory;
import org.mule.test.runner.maven.MavenModelFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;

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
 * {@link ArtifactClassLoader} for the application, plugins and container.
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
  private Set<String> excludedArtifacts = emptySet();
  private Set<String> providedExclusions = emptySet();
  private Set<String> testExclusions = emptySet();
  private Set<String> testInclusions = emptySet();
  private Set<String> sharedPluginLibCoordinates = emptySet();
  private Set<String> extraPrivilegedArtifacts = emptySet();
  private Set<Class> exportPluginClasses = emptySet();
  private boolean extensionMetadataGenerationEnabled = false;
  private Set<String> providedInclusions = emptySet();
  private List<URL> applicationUrls = newArrayList();
  private List<String> extraBootPackages;

  /**
   * Sets the {@link Set} of Maven coordinates in format {@code <groupId>:<artifactId>} in order to be added to the sharedLib
   * {@link ArtifactClassLoader}
   *
   * @param sharedPluginLibCoordinates {@link List} of Maven coordinates in format {@code <groupId>:<artifactId>}
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setSharedPluginLibCoordinates(Set<String> sharedPluginLibCoordinates) {
    this.sharedPluginLibCoordinates = sharedPluginLibCoordinates;
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
   * packages. In format {@code [groupId]:[artifactId]:[extension]:[classifier]:[version]}.
   *
   * @param excludedArtifacts Maven artifacts to be excluded from artifact class loaders created here due to they are going to be
   *        added as boot packages. In format {@code [groupId]:[artifactId]:[extension]:[classifier]:[version]}.
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setExcludedArtifacts(Set<String> excludedArtifacts) {
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
   * Sets the {@link List} of {@link String}s containing the extra privileged artifacts defined to be appended to the container in
   * addition to the pre-defined ones.
   *
   * @param extraPrivilegedArtifacts {@link List} of {@link String}s containing the extra privileged artifacts defined to be
   *        appended to the container in addition to the pre-defined ones.
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setExtraPrivilegedArtifacts(Set<String> extraPrivilegedArtifacts) {
    this.extraPrivilegedArtifacts = extraPrivilegedArtifacts;
    return this;
  }

  /**
   * Sets Maven artifacts to be excluded from the {@code provided} scope direct dependencies of the rootArtifact. In format
   * {@code [groupId]:[artifactId]:[extension]:[classifier]:[version]}.
   * <p/>
   *
   * @param providedExclusions Maven artifacts to be excluded from the {@code provided} scope direct dependencies of the
   *        rootArtifact. In format {@code [groupId]:[artifactId]:[extension]:[classifier]:[version]}.
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setProvidedExclusions(final Set<String> providedExclusions) {
    this.providedExclusions = providedExclusions;
    return this;
  }

  /**
   * Sets the {@link Set} of exclusion Maven coordinates to be excluded from test dependencies of rootArtifact. In format
   * {@code [groupId]:[artifactId]:[extension]:[classifier]:[version]}.
   *
   * @param testExclusions {@link Set} of exclusion Maven coordinates to be excluded from test dependencies of rootArtifact. In
   *        format {@code [groupId]:[artifactId]:[extension]:[classifier]:[version]}.
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setTestExclusions(final Set<String> testExclusions) {
    this.testExclusions = testExclusions;
    return this;
  }

  /**
   * Sets the {@link Set} of inclusion Maven coordinates to be included from test dependencies of rootArtifact. In format
   * {@code [groupId]:[artifactId]:[extension]:[classifier]:[version]}.
   *
   * @param testInclusions {@link Set} of inclusion Maven coordinates to be excluded from test dependencies of rootArtifact. In
   *        format {@code [groupId]:[artifactId]:[extension]:[classifier]:[version]}.
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setTestInclusions(final Set<String> testInclusions) {
    this.testInclusions = testInclusions;
    return this;
  }

  /**
   * Sets the {@link Set} of {@link Class}es to be exported by rootArtifact (if it is a Mule plugin) in addition to their APIs,
   * for testing purposes only.
   *
   * @param exportPluginClasses of {@link Class}es to be exported by rootArtifact (if it is a Mule plugin) in addition to their
   *        APIs, for testing purposes only.
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setExportPluginClasses(final Set<Class> exportPluginClasses) {
    this.exportPluginClasses = exportPluginClasses;
    return this;
  }

  /**
   * Sets to {@code true} if while building the a plugin {@link ArtifactClassLoader}
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
   * Sets a {@link List} of {@link URL}s to be appended to the application
   * {@link ArtifactClassLoader} in addition to the ones classified.
   *
   * @param {@link List} of {@link URL}s to be appended to the application
   *        {@link ArtifactClassLoader} in addition to the ones classified.
   * @return this
   */
  public ArtifactIsolatedClassLoaderBuilder setApplicationUrls(List<URL> applicationUrls) {
    this.applicationUrls = applicationUrls;
    return this;
  }

  /**
   * Builds the {@link ArtifactClassLoaderHolder} with the
   * {@link ArtifactClassLoader}s for application, plugins and container.
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
                                         testExclusions,
                                         testInclusions,
                                         sharedPluginLibCoordinates,
                                         exportPluginClasses,
                                         applicationUrls,
                                         extensionMetadataGenerationEnabled);
    } catch (IOException e) {
      throw new RuntimeException("Error while creating the classification context", e);
    }

    ArtifactsUrlClassification artifactsUrlClassification = classPathClassifier.classify(context);
    return isolatedClassLoaderFactory.createArtifactClassLoader(context.getExtraBootPackages(), extraPrivilegedArtifacts,
                                                                artifactsUrlClassification);
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
