/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.plugin;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.MAVEN;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.config.bootstrap.ArtifactType;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.descriptor.BundleScope;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel.ClassLoaderModelBuilder;
import org.mule.runtime.module.deployment.impl.internal.artifact.MavenClassLoaderModelLoader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible of returning the {@link BundleDescriptor} of a given plugin's location and also creating a
 * {@link ClassLoaderModel} TODO(fernandezlautaro): MULE-11094 this class is the default implementation for discovering
 * dependencies and URLs, which happens to be Maven based. There could be other ways to look for dependencies and URLs (probably
 * for testing purposes where the plugins are done by hand and without maven) which will imply implementing the jira pointed out
 * in this comment.
 *
 * @since 4.0
 */
public class PluginMavenClassLoaderModelLoader extends MavenClassLoaderModelLoader {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public String getId() {
    return MAVEN;
  }

  protected void loadDependencies(ClassLoaderModelBuilder classLoaderModelBuilder, DependencyResult dependencyResult,
                                  PreorderNodeListGenerator nlg) {
    // Looking for all Mule plugin dependencies
    final Set<BundleDependency> plugins = new HashSet<>();
    dependencyResult.getArtifactResults().stream()
        .forEach(dependency -> {
          BundleDescriptor.Builder builder = new BundleDescriptor.Builder()
              .setArtifactId(dependency.getArtifact().getArtifactId())
              .setGroupId(dependency.getArtifact().getGroupId())
              .setVersion(dependency.getArtifact().getVersion())
              .setType(dependency.getArtifact().getExtension());
          if (!isEmpty(dependency.getArtifact().getClassifier())) {
            builder = builder
                .setClassifier(dependency.getArtifact().getClassifier());
          }

          try {
            plugins.add(new BundleDependency.Builder()
                .setDescriptor(builder.build())
                .setScope(BundleScope.COMPILE)
                .setBundleUrl(dependency.getArtifact().getFile().toURL())
                .build());
          } catch (MalformedURLException e) {
            throw new MuleRuntimeException(e);
          }
        });
    classLoaderModelBuilder.dependingOn(plugins);
  }

  protected void loadUrls(File pluginFolder, ClassLoaderModelBuilder classLoaderModelBuilder,
                          DependencyResult dependencyResult, PreorderNodeListGenerator nlg) {
    // Adding the exploded JAR root folder
    classLoaderModelBuilder.containing(getUrl(pluginFolder, pluginFolder));

    nlg.getArtifacts(false).stream().forEach(artifact -> {
      // Adding all needed jar's file dependencies
      classLoaderModelBuilder.containing(getUrl(pluginFolder, artifact.getFile()));
    });
  }

  private URL getUrl(File pluginFolder, File file) {
    try {
      return file.toURI().toURL();
    } catch (MalformedURLException e) {
      throw new ArtifactDescriptorCreateException(format("There was an issue obtaining the URL for the plugin [%s], file [%s]",
                                                         pluginFolder.getAbsolutePath(), file.getAbsolutePath()),
                                                  e);
    }
  }

  @Override
  public boolean supportsArtifactType(ArtifactType artifactType) {
    return artifactType.equals(ArtifactType.PLUGIN);
  }
}
