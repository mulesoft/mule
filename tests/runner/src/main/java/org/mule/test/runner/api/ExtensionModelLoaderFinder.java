/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static org.mule.test.runner.utils.ExtensionLoaderUtils.getLoaderById;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.extension.api.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.test.runner.maven.MavenModelFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;

import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

/**
 * Finds the extension model loader for a given extension artifact
 *
 * @since 4.0
 */
class ExtensionModelLoaderFinder {

  public ExtensionModelLoader findLoaderFromMulePlugin(File extensionMulePluginJson) {
    try {
      MulePluginBasedLoaderFinder finder = new MulePluginBasedLoaderFinder(extensionMulePluginJson);
      return finder.getLoader();
    } catch (FileNotFoundException e) {
      // TODO: MULE-12295. make it work for soap connect extensions when running from IDE
      return new DefaultJavaExtensionModelLoader();
    }
  }

  /**
   * Searches in the plugin pom.xml for the {@code testExtensionModelLoaderId} property which specifies with which loader the
   * extension must be loaded. The main use of this is for Test Extensions that don't generate a mule-artifact.json.
   */
  public Optional<ExtensionModelLoader> findLoaderByProperty(Artifact plugin, DependencyResolver dependencyResolver,
                                                             List<RemoteRepository> rootArtifactRemoteRepositories) {
    DefaultArtifact artifact = new DefaultArtifact(plugin.getGroupId(), plugin.getArtifactId(), "pom", plugin.getVersion());
    try {
      ArtifactResult artifactResult = dependencyResolver.resolveArtifact(artifact, rootArtifactRemoteRepositories);
      File pomFile = artifactResult.getArtifact().getFile();
      Model mavenProject = MavenModelFactory.createMavenProject(pomFile);
      String id = mavenProject.getProperties().getProperty("testExtensionModelLoaderId");
      return id != null ? Optional.ofNullable(getLoaderById(id)) : Optional.empty();
    } catch (ArtifactResolutionException e) {
      throw new RuntimeException("Cannot load extension, the artifact: [" + plugin.toString() + "] cannot be resolved", e);
    }
  }
}
