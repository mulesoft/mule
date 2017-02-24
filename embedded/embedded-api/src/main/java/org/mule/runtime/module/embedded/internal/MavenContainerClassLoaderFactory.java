/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.embedded.internal;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.module.embedded.internal.MavenUtils.loadUrls;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.function.Predicate;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;

/**
 * Creates a {@link ClassLoader} with the {@link URL}'s for the Container.
 *
 * @since 4.0
 */
public class MavenContainerClassLoaderFactory {

  private static final String CONTAINER_BOM_GROUP_ID = "org.mule";
  private static final String CONTAINER_BOM_ARTIFACT_ID = "mule-runtime-impl-bom";

  private final Repository repository;

  public MavenContainerClassLoaderFactory(Repository repository) {
    this.repository = repository;
  }

  /**
   * Creates the {@link ClassLoader} Container for a given version.
   *
   * @param version Maven version. Not null.
   * @return a {@link ClassLoader} Container.
   */
  public ClassLoader create(String version, ClassLoader parentClassLoader) throws ArtifactResolutionException {
    Artifact defaultArtifact = getContainerBomArtifact(version);

    Predicate<Dependency> zipDependencyFilter =
        dependency -> !dependency.getArtifact().getArtifactId().equals("mule-services-all");
    PreorderNodeListGenerator nlg =
        repository.assemblyDependenciesForArtifact(defaultArtifact, zipDependencyFilter);
    List<URL> urls = loadUrls(nlg);

    return new URLClassLoader(urls.toArray(new URL[0]), parentClassLoader);
  }

  public List<URL> getServices(String version) {
    Artifact defaultArtifact = getContainerBomArtifact(version);

    final PreorderNodeListGenerator nlg =
        repository.assemblyDependenciesForArtifact(defaultArtifact,
                                                   dependency -> dependency.getArtifact().getArtifactId()
                                                       .equals("mule-services-all"));
    List<URL> urls = loadUrls(nlg);
    return urls.stream().filter(u -> u.getFile().endsWith(".zip")).collect(toList());
  }

  private Artifact getContainerBomArtifact(String version) {
    return new DefaultArtifact(CONTAINER_BOM_GROUP_ID, CONTAINER_BOM_ARTIFACT_ID,
                               null,
                               "pom",
                               version);
  }


}
