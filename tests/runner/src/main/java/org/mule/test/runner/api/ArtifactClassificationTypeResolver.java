/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static org.mule.runtime.core.util.Preconditions.checkNotNull;
import static org.mule.test.runner.api.ArtifactClassificationType.APPLICATION;
import static org.mule.test.runner.api.ArtifactClassificationType.MODULE;
import static org.mule.test.runner.api.ArtifactClassificationType.PLUGIN;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves the {@link ArtifactClassificationType} for an {@link org.eclipse.aether.artifact.Artifact}.
 *
 * @since 4.0
 */
public class ArtifactClassificationTypeResolver {

  private static final String MULE_PLUGIN_CLASSIFIER = "mule-plugin";
  private static final String MULE_EXTENSION_CLASSIFIER = "mule-extension";
  private static final String MULE_MODULE_PROPERTIES = "META-INF/mule-module.properties";
  private static final String JAR_EXTENSION = "jar";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private DependencyResolver dependencyResolver;

  /**
   * Creates an instance of this resolver.
   *
   * @param dependencyResolver {@link DependencyResolver} to get artifact output.
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
    if (isMulePlugin(artifact)) {
      return PLUGIN;
    }
    if (isMuleModule(artifact)) {
      return MODULE;
    }
    return APPLICATION;
  }

  private boolean isMulePlugin(Artifact artifact) {
    return artifact.getExtension().equals(MULE_PLUGIN_CLASSIFIER) || artifact.getExtension().equals(MULE_EXTENSION_CLASSIFIER);
  }

  private boolean isMuleModule(Artifact artifact) {
    URL url = resolveRootArtifactUrls(artifact);
    if (url == null) {
      return false;
    }
    return new URLClassLoader(new URL[] {url}, null).getResource(MULE_MODULE_PROPERTIES) != null;
  }

  /**
   * Resolves the rootArtifact {@link URL}s resources to be added to class loader.
   *
   * @param artifact {@link Artifact} being classified
   * @return {@link List} of {@link URL}s to be added to class loader
   */
  private URL resolveRootArtifactUrls(Artifact artifact) {
    final DefaultArtifact jarArtifact = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(),
                                                            JAR_EXTENSION, JAR_EXTENSION, artifact.getVersion());
    try {
      return dependencyResolver.resolveArtifact(jarArtifact).getArtifact().getFile().toURI().toURL();
    } catch (MalformedURLException e) {
      throw new IllegalStateException("Couldn't generate the URL for artifact: " + artifact);
    } catch (ArtifactResolutionException e) {
      logger.debug("'{}' artifact doesn't generate a JAR output", artifact);
      return null;
    }
  }
}
