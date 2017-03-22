/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static java.lang.Boolean.getBoolean;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.core.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.config.bootstrap.ArtifactType.POLICY;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.MAVEN;
import static org.mule.runtime.module.artifact.classloader.MuleMavenPlugin.MULE_MAVEN_PLUGIN_ARTIFACT_ID;
import static org.mule.runtime.module.artifact.classloader.MuleMavenPlugin.MULE_MAVEN_PLUGIN_GROUP_ID;
import static org.mule.runtime.module.artifact.descriptor.BundleScope.COMPILE;
import static org.mule.runtime.module.deployment.impl.internal.plugin.MavenUtils.getPomModelFolder;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.core.config.bootstrap.ArtifactType;
import org.mule.runtime.module.artifact.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel.ClassLoaderModelBuilder;
import org.mule.runtime.module.artifact.util.FileJarExplorer;
import org.mule.runtime.module.artifact.util.JarInfo;
import org.mule.runtime.module.deployment.impl.internal.artifact.MavenClassLoaderModelLoader;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible of returning the {@link BundleDescriptor} of a given plugin's location and also creating a
 * {@link ClassLoaderModel}
 * <p>
 * TODO(fernandezlautaro): MULE-11094 this class is the default implementation for discovering dependencies and URLs, which
 * happens to be Maven based. There could be other ways to look for dependencies and URLs (probably for testing purposes where the
 * plugins are done by hand and without maven) which will imply implementing the jira pointed out in this comment.
 *
 * @since 4.0
 */
public class DeployableMavenClassLoaderModelLoader extends MavenClassLoaderModelLoader {

  public static final String ADD_TEST_DEPENDENCIES_KEY = "mule.embedded.maven.addTestDependenciesToAppClassPath";
  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public String getId() {
    return MAVEN;
  }

  protected Set<BundleDependency> loadDependencies(DependencyResult dependencyResult, PreorderNodeListGenerator nlg) {
    final Set<BundleDependency> dependencies = new HashSet<>();
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
            dependencies.add(builder.build());
          } catch (MalformedURLException e) {
            throw new MuleRuntimeException(e);
          }
        });
    return dependencies;
  }

  protected void loadUrls(File applicationFolder, ClassLoaderModelBuilder classLoaderModelBuilder,
                          DependencyResult dependencyResult, PreorderNodeListGenerator nlg, Set<BundleDependency> dependencies) {
    // Adding the exploded JAR root folder
    try {
      classLoaderModelBuilder.containing(new File(applicationFolder, "classes").toURL());
      addDependenciesToClasspathUrls(classLoaderModelBuilder, dependencyResult);
      exportSharedLibrariesResouresAndPackages(applicationFolder, classLoaderModelBuilder, dependencies);
    } catch (MalformedURLException e) {
      throw new MuleRuntimeException(e);
    }
  }

  private void exportSharedLibrariesResouresAndPackages(File applicationFolder, ClassLoaderModelBuilder classLoaderModelBuilder,
                                                        Set<BundleDependency> dependencies) {
    Model model = loadPomModel(applicationFolder);
    Build build = model.getBuild();
    if (build != null) {
      List<Plugin> plugins = build.getPlugins();
      if (plugins != null) {
        Optional<Plugin> packagingPluginOptional =
            plugins.stream().filter(plugin -> plugin.getArtifactId().equals(MULE_MAVEN_PLUGIN_ARTIFACT_ID)
                && plugin.getGroupId().equals(MULE_MAVEN_PLUGIN_GROUP_ID)).findFirst();
        packagingPluginOptional.ifPresent(packagingPlugin -> {
          Xpp3Dom sharedLibrariesDom = ((Xpp3Dom) packagingPlugin.getConfiguration()).getChild("sharedLibraries");
          if (sharedLibrariesDom != null) {
            Xpp3Dom[] sharedLibraries = sharedLibrariesDom.getChildren("sharedLibrary");
            if (sharedLibraries != null) {
              FileJarExplorer fileJarExplorer = new FileJarExplorer();
              for (Xpp3Dom sharedLibrary : sharedLibraries) {
                String groupId = getSharedLibraryAttribute(applicationFolder, sharedLibrary, "groupId");
                String artifactId = getSharedLibraryAttribute(applicationFolder, sharedLibrary, "artifactId");
                Optional<BundleDependency> bundleDependencyOptional = dependencies.stream()
                    .filter(bundleDependency -> bundleDependency.getDescriptor().getArtifactId().equals(artifactId)
                        && bundleDependency.getDescriptor().getGroupId().equals(groupId))
                    .findFirst();
                bundleDependencyOptional.map(bundleDependency -> {
                  JarInfo jarInfo = fileJarExplorer.explore(bundleDependency.getBundleUrl());
                  classLoaderModelBuilder.exportingPackages(jarInfo.getPackages());
                  classLoaderModelBuilder.exportingResources(jarInfo.getResources());
                  return bundleDependency;
                }).orElseThrow(() -> new MuleRuntimeException(I18nMessageFactory
                    .createStaticMessage(format(
                                                "Dependency %s:%s could not be found within the artifact %s. It must be declared within the maven dependencies of the artifact.",
                                                groupId,
                                                artifactId, applicationFolder.getName()))));
              }
            }
          }
        });
      }
    }
  }

  private void addDependenciesToClasspathUrls(ClassLoaderModelBuilder classLoaderModelBuilder,
                                              DependencyResult dependencyResult) {
    dependencyResult.getArtifactResults().stream()
        .filter(artifactResult -> !MULE_PLUGIN_CLASSIFIER.equals(artifactResult.getArtifact().getClassifier()))
        .forEach(artifactResult -> {
          try {
            classLoaderModelBuilder.containing(artifactResult.getArtifact().getFile().toURL());
          } catch (MalformedURLException e) {
            throw new MuleRuntimeException(e);
          }
        });
  }

  private String getSharedLibraryAttribute(File applicationFolder, Xpp3Dom sharedLibraryDom, String attributeName) {
    Xpp3Dom attributeDom = sharedLibraryDom.getChild(attributeName);
    checkState(attributeDom != null,
               format("%s element was not defined within the shared libraries declared in the pom file of the artifact %s",
                      attributeName, applicationFolder.getName()));
    String attributeValue = attributeDom.getValue().trim();
    checkState(!isEmpty(attributeValue),
               format("%s was defined but has an empty value within the shared libraries declared in the pom file of the artifact %s",
                      attributeName, applicationFolder.getName()));
    return attributeValue;
  }

  @Override
  protected boolean enabledTestDependencies() {
    return getBoolean(ADD_TEST_DEPENDENCIES_KEY);
  }

  @Override
  protected Model loadPomModel(File artifactFile) {
    return getPomModelFolder(artifactFile);
  }

  @Override
  public boolean supportsArtifactType(ArtifactType artifactType) {
    return artifactType.equals(APP) || artifactType.equals(POLICY);
  }
}
