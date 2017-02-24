/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static java.lang.Boolean.getBoolean;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.runtime.core.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.MAVEN;
import static org.mule.runtime.module.artifact.descriptor.BundleScope.COMPILE;
import static org.mule.runtime.module.deployment.impl.internal.plugin.MavenUtils.getPomModelFolder;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.config.bootstrap.ArtifactType;
import org.mule.runtime.module.artifact.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel.ClassLoaderModelBuilder;
import org.mule.runtime.module.deployment.impl.internal.artifact.MavenClassLoaderModelLoader;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.model.Model;
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
public class AppMavenClassLoaderModelLoader extends MavenClassLoaderModelLoader {

  public static final String ADD_TEST_DEPENDENCIES_KEY = "mule.embedded.maven.addTestDependenciesToAppClassPath";
  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public String getId() {
    return MAVEN;
  }

  protected void loadDependencies(ClassLoaderModelBuilder classLoaderModelBuilder, DependencyResult dependencyResult,
                                  PreorderNodeListGenerator nlg) {
    final Set<BundleDependency> plugins = new HashSet<>();
    nlg.getDependencies(true).stream()
        .forEach(dependency -> {
          final BundleDescriptor.Builder bundleDescriptorBuilder = new BundleDescriptor.Builder()
              .setArtifactId(dependency.getArtifact().getArtifactId())
              .setGroupId(dependency.getArtifact().getGroupId())
              .setVersion(dependency.getArtifact().getVersion())
              .setType(dependency.getArtifact().getExtension());

          String classifier = dependency.getArtifact().getClassifier();
          if (!isEmpty(classifier)) {
            bundleDescriptorBuilder.setClassifier(classifier);
          }

          try {
            BundleDependency.Builder builder = new BundleDependency.Builder()
                .setDescriptor(bundleDescriptorBuilder.build())
                .setScope(COMPILE);
            if (!dependency.getScope().equalsIgnoreCase("provided")) {
              builder.setBundleUrl(dependency.getArtifact().getFile().toURI().toURL());
            }
            plugins.add(builder.build());
          } catch (MalformedURLException e) {
            throw new MuleRuntimeException(e);
          }
        });
    classLoaderModelBuilder.dependingOn(plugins);
  }

  protected void loadUrls(File applicationFolder, ClassLoaderModelBuilder classLoaderModelBuilder,
                          DependencyResult dependencyResult, PreorderNodeListGenerator nlg) {
    // Adding the exploded JAR root folder
    try {
      classLoaderModelBuilder.containing(new File(applicationFolder, "classes").toURL());
      dependencyResult.getArtifactResults().stream()
          .filter(artifactResult -> !MULE_PLUGIN_CLASSIFIER.equals(artifactResult.getArtifact().getClassifier()))
          .forEach(artifactResult -> {
            try {
              classLoaderModelBuilder.containing(artifactResult.getArtifact().getFile().toURL());
            } catch (MalformedURLException e) {
              throw new MuleRuntimeException(e);
            }
          });
    } catch (MalformedURLException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  protected boolean enabledTestDependencies() {
    return getBoolean(ADD_TEST_DEPENDENCIES_KEY);
  }

  @Override
  protected Model getPomModel(File artifactFile) {
    return getPomModelFolder(artifactFile);
  }

  @Override
  public boolean supportsArtifactType(ArtifactType artifactType) {
    return artifactType.equals(APP);
  }
}
