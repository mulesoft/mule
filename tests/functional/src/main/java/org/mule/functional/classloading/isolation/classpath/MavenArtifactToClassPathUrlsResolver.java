/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.classloading.isolation.classpath;

import org.mule.functional.api.classloading.isolation.MavenArtifact;
import org.mule.functional.api.classloading.isolation.MavenMultiModuleArtifactMapping;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Optional;

/**
 * Resolves a {@link MavenArtifact} by selecting from the list of URLs the one that matches. It supports artifacts already
 * packages (for CI environments) and multi-module maven projects.
 *
 * @since 4.0
 */
public class MavenArtifactToClassPathUrlsResolver {

  private final MavenMultiModuleArtifactMapping mavenMultiModuleArtifactMapping;

  /**
   * Creates an instance of the resolver that uses a {@link MavenMultiModuleArtifactMapping} mapper for multi-module artifacts.
   *
   * @param mavenMultiModuleArtifactMapping the mapper for multi-module artifacts.
   */
  public MavenArtifactToClassPathUrlsResolver(MavenMultiModuleArtifactMapping mavenMultiModuleArtifactMapping) {
    this.mavenMultiModuleArtifactMapping = mavenMultiModuleArtifactMapping;
  }

  /**
   * Looks for a matching {@link URL} for the artifact.
   *
   * @param artifact to be used in order to find the {@link URL} in list of urls
   * @param urls a list of {@link URL} provided by the classpath
   * @throws IllegalArgumentException if the artifact couldn't be resolved to a URL
   * @return a non-null {@link URL} that represents the {@link MavenArtifact} passed
   */
  public URL resolveURL(final MavenArtifact artifact, final List<URL> urls) {
    Optional<URL> artifactURL = urls.stream().filter(filePath -> filePath.getFile()
        .contains(artifact.getGroupIdAsPath() + File.separator + artifact.getArtifactId() + File.separator)).findFirst();
    if (artifactURL.isPresent()) {
      return artifactURL.get();
    } else {
      return getModuleURL(artifact, urls);
    }
  }

  /**
   * Looks for a matching {@link URL} for the artifact but resolving it as multi-module artifact. It also supports to look for
   * jars or classes depending if the artifacts were packaged or not.
   *
   * @param artifact to be used in order to find the {@link URL} in list of urls
   * @param urls a list of {@link URL} obtained from the classpath
   * @throws IllegalArgumentException if couldn't find a mapping URL either
   * @return a non-null {@link URL} that represents the {@link MavenArtifact} passed
   */
  private URL getModuleURL(final MavenArtifact artifact, final List<URL> urls) {
    final StringBuilder moduleFolder =
        new StringBuilder(mavenMultiModuleArtifactMapping.getFolderName(artifact.getArtifactId())).append("target/");

    // Fix to handle when running test during an install phase due to maven builds the classpath pointing out to packaged files
    // instead of classes folders.
    final StringBuilder explodedUrlSuffix = new StringBuilder();
    final StringBuilder packagedUrlSuffix = new StringBuilder();
    if (artifact.isTestScope() && artifact.getType().equals("test-jar")) {
      explodedUrlSuffix.append("test-classes/");
      packagedUrlSuffix.append(".*-tests.jar");
    } else {
      explodedUrlSuffix.append("classes/");
      packagedUrlSuffix.append("^(?!.*?(?:-tests.jar)).*.jar");
    }
    final Optional<URL> localFile = urls.stream().filter(url -> {
      String path = url.getFile();
      if (path.contains(moduleFolder)) {
        String pathSuffix = path.substring(path.lastIndexOf(moduleFolder.toString()) + moduleFolder.length(), path.length());
        return pathSuffix.matches(explodedUrlSuffix.toString()) || pathSuffix.matches(packagedUrlSuffix.toString());
      }
      return false;
    }).findFirst();
    if (!localFile.isPresent()) {
      throw new IllegalArgumentException("Cannot locate artifact as multi-module dependency: '" + artifact
          + "', on module folder: " + moduleFolder + " using exploded url suffix regex: " + explodedUrlSuffix + " or "
          + packagedUrlSuffix + " using classpath: " + urls);

    }
    return localFile.get();
  }

}
