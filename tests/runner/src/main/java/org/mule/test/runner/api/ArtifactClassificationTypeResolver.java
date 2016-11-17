/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.test.runner.api.ArtifactClassificationType.APPLICATION;
import static org.mule.test.runner.api.ArtifactClassificationType.MODULE;
import static org.mule.test.runner.api.ArtifactClassificationType.PLUGIN;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;

/**
 * Resolves the {@link ArtifactClassificationType} for an {@link org.eclipse.aether.artifact.Artifact}.
 *
 * @since 4.0
 */
public class ArtifactClassificationTypeResolver {

  private static final String MULE_EXTENSION_CLASSIFIER = "mule-extension";
  private static final String MULE_MODULE_PROPERTIES = "META-INF/mule-module.properties";
  private static final String PLUGIN_PROPERTIES = "plugin.properties";
  private static final String JAR_EXTENSION = "jar";

  private DependencyResolver dependencyResolver;

  /**
   * Creates an instance of this resolver.
   *
   * @param dependencyResolver {@link DependencyResolver} to get artifact output. Non null.
   */
  public ArtifactClassificationTypeResolver(DependencyResolver dependencyResolver) {
    checkNotNull(dependencyResolver, "dependencyResolver cannot be null");

    this.dependencyResolver = dependencyResolver;
  }

  /**
   * Resolves the {@link ArtifactClassificationType} for the artifact.
   *
   * @param artifact the {@link Artifact} to get its {@link ArtifactClassificationType}
   * @return {@link ArtifactClassificationType} for the artifact given
   */
  public ArtifactClassificationType resolveArtifactClassificationType(Artifact artifact) {
    try (URLClassLoader artifactClassLoader = createArtifactClassLoader(artifact)) {
      if (isMulePlugin(artifact, artifactClassLoader)) {
        return PLUGIN;
      }
      if (isMuleModule(artifactClassLoader)) {
        return MODULE;
      }
      return APPLICATION;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * @param artifact {@link Artifact} to check if it is a plugin
   * @param artifactClassLoader {@link ClassLoader} for the given artifact
   * @return true if it is classified as {@value org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor2#MULE_PLUGIN_CLASSIFIER} or {@value #MULE_EXTENSION_CLASSIFIER} or it has a
   *         {@value #PLUGIN_PROPERTIES} file.
   */
  private boolean isMulePlugin(Artifact artifact, ClassLoader artifactClassLoader) {
    return artifact.getExtension().equals(MULE_PLUGIN_CLASSIFIER) || artifact.getExtension().equals(MULE_EXTENSION_CLASSIFIER)
        || hasResource(artifactClassLoader, PLUGIN_PROPERTIES);
  }

  /**
   * @param artifactClassLoader {@link ClassLoader} for the given artifact.
   * @return true if it has a {@value #MULE_MODULE_PROPERTIES} file.
   */
  private boolean isMuleModule(ClassLoader artifactClassLoader) {
    return hasResource(artifactClassLoader, MULE_MODULE_PROPERTIES);
  }

  /**
   * Checks if the {@link ClassLoader} has the resource given.
   *
   * @param classLoader {@link ClassLoader} in order to check if the resource exists, can be null.
   * @param resource name of the resource to look for.
   * @return
   */
  private boolean hasResource(ClassLoader classLoader, String resource) {
    if (classLoader == null) {
      return false;
    }
    return classLoader.getResource(resource) != null;
  }

  private URLClassLoader createArtifactClassLoader(Artifact artifact) {
    try {
      return new URLClassLoader(new URL[] {resolveRootArtifactUrls(artifact)}, null);
    } catch (ArtifactResolutionException e) {
      return null;
    }
  }

  /**
   * Resolves the rootArtifact {@link URL}s resources to be added to class loader.
   *
   * @param artifact {@link Artifact} being classified
   * @return {@link List} of {@link URL}s to be added to class loader
   * @throws {@link ArtifactResolutionException} if the artifact doesn't have a JAR output file (target/classes when not packaged)
   */
  private URL resolveRootArtifactUrls(Artifact artifact) throws ArtifactResolutionException {
    final DefaultArtifact jarArtifact = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(),
                                                            JAR_EXTENSION, JAR_EXTENSION, artifact.getVersion());
    try {
      return dependencyResolver.resolveArtifact(jarArtifact).getArtifact().getFile().toURI().toURL();
    } catch (MalformedURLException e) {
      throw new IllegalStateException("Couldn't generate the URL for artifact: " + artifact);
    }
  }
}
