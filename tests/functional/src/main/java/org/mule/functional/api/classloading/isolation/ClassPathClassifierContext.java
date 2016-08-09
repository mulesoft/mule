/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.api.classloading.isolation;

import static org.mule.functional.classloading.isolation.utils.RunnerModuleUtils.getExcludedProperties;
import static org.mule.runtime.core.util.Preconditions.checkNotNull;
import org.mule.functional.classloading.isolation.maven.MavenArtifactMatcherPredicate;

import com.google.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a context that contains what is needed in order to do a classpath classification. It is used in
 * {@link ClassPathClassifier}.
 *
 * @since 4.0
 */
public class ClassPathClassifierContext {

  public static final int GROUP_ID_ARTIFACT_ID_TYPE_PATTERN_CHUNKS = 3;
  public static final String EXCLUDED_MODULES = "excluded.modules";

  private final File rootArtifactClassesFolder;
  private final File rootArtifactTestClassesFolder;
  private final List<URL> classPathURLs;
  private final DependenciesGraph dependenciesGraph;
  private final MavenMultiModuleArtifactMapping mavenMultiModuleArtifactMapping;
  private final Predicate<MavenArtifact> exclusions;
  private final Set<String> extraBootPackages;
  private final List<String> extensionBasePackages;
  private final Set<Class> exportClasses;

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  /**
   * Creates a context used for doing the classification of the class path.
   *
   * @param rootArtifactClassesFolder {@link File} to the target/classes of the current artifact being classified. Not null.
   * @param rootArtifactTestClassesFolder {@link File} to the target/test-classes of the current artifact being classified. Not
   *        null.
   * @param classPathURLs the whole set of {@link URL}s that were loaded by IDE/Maven Surefire plugin when running the test. Not
   *        null.
   * @param dependenciesGraph the maven dependencies graph for the artifact that the test belongs to. Not null.
   * @param mavenMultiModuleArtifactMapping a mapper to get multi-module folder for artifactIds. Not null.
   * @param exclusionsList {@link List} of {@link String}'s to be used during classification for excluded packages.
   * @param extraBootPackagesList {@link List} of {@link String}'s packages to be added as boot packages to the container.
   * @param extensionBasePackages {@link List} of {@link String}'s base packages to be used for discovering extensions.
   * @param exportClasses {@link Set} of {@link Class} to be exported in addition to the ones already exported by the plugin, for
   *        testing purposes only.
   * @throws IOException if an error happened while reading
   *         {@link org.mule.functional.classloading.isolation.utils.RunnerModuleUtils#EXCLUDED_PROPERTIES_FILE} file
   */
  public ClassPathClassifierContext(final File rootArtifactClassesFolder, final File rootArtifactTestClassesFolder,
                                    final List<URL> classPathURLs, final DependenciesGraph dependenciesGraph,
                                    final MavenMultiModuleArtifactMapping mavenMultiModuleArtifactMapping,
                                    final List<String> exclusionsList, final List<String> extraBootPackagesList,
                                    final List<String> extensionBasePackages, final Set<Class> exportClasses)
      throws IOException {
    checkNotNull(rootArtifactClassesFolder, "rootArtifactClassesFolder cannot be null");
    checkNotNull(rootArtifactTestClassesFolder, "rootArtifactTestClassesFolder cannot be null");
    checkNotNull(classPathURLs, "classPathURLs cannot be null");
    checkNotNull(dependenciesGraph, "dependenciesGraph cannot be null");
    checkNotNull(mavenMultiModuleArtifactMapping, "mavenMultiModuleArtifactMapping cannot be null");

    this.rootArtifactClassesFolder = rootArtifactClassesFolder;
    this.rootArtifactTestClassesFolder = rootArtifactTestClassesFolder;
    this.classPathURLs = classPathURLs;
    this.dependenciesGraph = dependenciesGraph;
    this.mavenMultiModuleArtifactMapping = mavenMultiModuleArtifactMapping;

    Properties excludedProperties = getExcludedProperties();
    this.exclusions = createExclusionsPredicate(exclusionsList, excludedProperties);
    this.extraBootPackages = getExtraBootPackages(extraBootPackagesList, excludedProperties);

    this.extensionBasePackages = extensionBasePackages;

    this.exportClasses = exportClasses;
  }

  /**
   * @return a {@link File} to the classes of the current artifact being tested.
   */
  public File getRootArtifactClassesFolder() {
    return rootArtifactClassesFolder;
  }

  /**
   * @return a {@link File} to the test classes of the current artifact being tested.
   */
  public File getRootArtifactTestClassesFolder() {
    return rootArtifactTestClassesFolder;
  }

  /**
   * @return a {@link List} of {@link URL}s for the classpath provided by JUnit (it is the complete list of URLs)
   */
  public List<URL> getClassPathURLs() {
    return classPathURLs;
  }

  /**
   * @return a {@link DependenciesGraph} for the given artifact tested.
   */
  public DependenciesGraph getDependencyGraph() {
    return dependenciesGraph;
  }

  /**
   * @return {@link MavenMultiModuleArtifactMapping} mapper for artifactIds and multi-module folders.
   */
  public MavenMultiModuleArtifactMapping getMavenMultiModuleArtifactMapping() {
    return mavenMultiModuleArtifactMapping;
  }

  /**
   * @return {@link Predicate} to be used to exclude artifacts from being added to application {@link ClassLoader} due to they are
   *         going to be in container {@link ClassLoader}.
   */
  public Predicate<MavenArtifact> getExclusions() {
    return exclusions;
  }

  /**
   * @return {@link Set} of {@link String}s containing the extra boot packages defined to be appended to the container in addition
   *         to the pre-defined ones.
   */
  public Set<String> getExtraBootPackages() {
    return extraBootPackages;
  }

  /**
   * @return a {@link List} of base packages to be used for discovering extensions in classpath.
   */
  public List<String> getExtensionBasePackages() {
    return extensionBasePackages;
  }

  /**
   * @return {@link Set} of {@link Class}es that are going to be exported in addition to the ones already exported by extensions.
   *         For testing purposes only.
   */
  public Set<Class> getExportClasses() {
    return exportClasses;
  }

  /**
   * The list of exclusion GroutId/ArtifactId/Type to be excluded from application/plugin class loaders due to these are supposed
   * to be exposed by the container.
   * <p/>
   * It defined by the file {@link org.mule.functional.classloading.isolation.utils.RunnerModuleUtils#EXCLUDED_PROPERTIES_FILE}
   * and can be changed by having this file in the module that is tested or appended to the default excluded
   * groutId/artifactId/type with the ones provided to this context.
   *
   * @param exclusionsList {@link List} of {@link String}'s, each entry is a coma separated list of patterns to parse and generate
   *        exclusions for.
   * @param excludedProperties {@link Properties} that has the list of excluded modules
   * @return a {@link Predicate} to be used in order to excluded maven artifacts from application/plugin class loaders.
   */
  private Predicate<MavenArtifact> createExclusionsPredicate(final List<String> exclusionsList, Properties excludedProperties) {
    Predicate<MavenArtifact> exclusionPredicate = null;
    String excludedModules = excludedProperties.getProperty(EXCLUDED_MODULES);
    if (excludedModules != null) {
      for (String exclusion : excludedModules.split(",")) {
        exclusionPredicate = createPredicate(exclusionPredicate, exclusion);
      }
    } else {
      logger.warn(EXCLUDED_MODULES
          + " found but there is no list of modules defined to be excluded, this could be the reason why the test may fail later due to JUnit classes are not found");
    }
    for (String exclusionsToBeAppended : exclusionsList) {
      if (exclusionsToBeAppended != null && exclusionsToBeAppended.length() > 0) {
        exclusionPredicate = createPredicate(exclusionPredicate, exclusionsToBeAppended);
      }
    }

    // If no exclusion is defined the predicate should always return false to any artifact due to none is excluded
    return exclusionPredicate == null ? x -> false : exclusionPredicate;
  }

  /**
   * Creates the predicate or adds a new one to the given one by splitting the exclusions patterns.
   *
   * @param exclusionPredicate the current exclusion predicate to compose with an OR operation (if not null).
   * @param exclusion value to be parse and generate exclusions for.
   * @return a new {@link Predicate} with the exclusions.
   */
  private Predicate<MavenArtifact> createPredicate(final Predicate<MavenArtifact> exclusionPredicate, final String exclusion) {
    Predicate<MavenArtifact> predicate = exclusionPredicate;
    String[] exclusionSplit = exclusion.split(":");
    if (exclusionSplit.length != GROUP_ID_ARTIFACT_ID_TYPE_PATTERN_CHUNKS) {
      throw new IllegalArgumentException("Exclusion pattern should have the format groupId:artifactId:type");
    }
    Predicate<MavenArtifact> artifactExclusion =
        new MavenArtifactMatcherPredicate(exclusionSplit[0], exclusionSplit[1], exclusionSplit[2]);
    if (predicate == null) {
      predicate = artifactExclusion;
    } else {
      predicate = predicate.or(artifactExclusion);
    }
    return predicate;
  }

  /**
   * Gets the {@link Set} of {@link String}s of packages to be added to the container {@link ClassLoader} in addition to the ones
   * already pre-defined by the mule container.
   *
   * @param excludedProperties {@link Properties }that has the list of extra boot packages definitions
   * @return a {@link Set} of {@link String}s with the extra boot packages to be appended
   */
  private Set<String> getExtraBootPackages(final List<String> extraBootPackagesList, final Properties excludedProperties) {
    Set<String> packages = Sets.newHashSet();
    packages.addAll(extraBootPackagesList);

    String excludedExtraBootPackages = excludedProperties.getProperty("extraBoot.packages");
    if (excludedExtraBootPackages != null) {
      for (String extraBootPackage : excludedExtraBootPackages.split(",")) {
        packages.add(extraBootPackage);
      }
    } else {
      logger.warn(EXCLUDED_MODULES
          + " found but there is no list of extra boot packages defined to be added to container, this could be the reason why the test may fail later due to JUnit classes are not found");
    }
    return packages;
  }

}
