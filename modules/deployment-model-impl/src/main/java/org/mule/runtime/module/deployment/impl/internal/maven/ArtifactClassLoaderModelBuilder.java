/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.maven;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.module.artifact.api.classloader.MuleMavenPlugin.MULE_MAVEN_PLUGIN_ARTIFACT_ID;
import static org.mule.runtime.module.artifact.api.classloader.MuleMavenPlugin.MULE_MAVEN_PLUGIN_GROUP_ID;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenUtils.getPomModelFolder;
import static org.mule.tools.api.classloader.Constants.ADDITIONAL_PLUGIN_DEPENDENCIES_FIELD;
import static org.mule.tools.api.classloader.Constants.PLUGIN_DEPENDENCIES_FIELD;
import static org.mule.tools.api.classloader.Constants.PLUGIN_DEPENDENCY_FIELD;
import static org.mule.tools.api.classloader.Constants.PLUGIN_FIELD;
import static org.mule.tools.api.classloader.Constants.SHARED_LIBRARIES_FIELD;
import static org.mule.tools.api.classloader.Constants.SHARED_LIBRARY_FIELD;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.internal.util.FileJarExplorer;
import org.mule.runtime.module.artifact.internal.util.JarInfo;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * ClassLoaderModelBuilder that adds the concept of Shared Library for the configured dependencies.
 *
 * @since 4.2.0
 */
public abstract class ArtifactClassLoaderModelBuilder extends ClassLoaderModel.ClassLoaderModelBuilder {

  private static final String GROUP_ID = "groupId";
  private static final String ARTIFACT_ID = "artifactId";
  private static final String VERSION = "version";
  private static final String MULE_PLUGIN = "mule-plugin";

  private boolean processSharedLibraries = false;
  private boolean processAdditionalPluginLibraries = false;
  protected File artifactFolder;
  private FileJarExplorer fileJarExplorer = new FileJarExplorer();

  public ArtifactClassLoaderModelBuilder(File artifactFolder) {
    requireNonNull(artifactFolder, "artifactFolder cannot be null");

    this.artifactFolder = artifactFolder;
  }

  /**
   * Sets a flag to export the configured shared libraries when building the ClassLoaderModel
   * @return this builder
   * @since 4.2.0
   */
  public ClassLoaderModel.ClassLoaderModelBuilder exportingSharedLibraries() {
    this.processSharedLibraries = true;
    return this;
  }

  /**
   * Sets a flag to include additional dependencies for each plugin if the deployable artifact defines them.
   * @since 4.2.0
   */
  public ClassLoaderModel.ClassLoaderModelBuilder additionalPluginLibraries() {
    this.processAdditionalPluginLibraries = true;
    return this;
  }

  @Override
  public ClassLoaderModel build() {
    Optional<Plugin> pluginOptional = empty();
    if (processSharedLibraries || processAdditionalPluginLibraries) {
      pluginOptional = findMuleMavenPluginDeclaration();
    }
    if (processSharedLibraries) {
      pluginOptional.ifPresent(plugin -> exportSharedLibrariesResourcesAndPackages(plugin));
    }
    if (processAdditionalPluginLibraries) {
      pluginOptional.ifPresent(plugin -> processAdditionalPluginLibraries(plugin));
    }
    return super.build();
  }

  private Optional<Plugin> findMuleMavenPluginDeclaration() {
    Model model = getPomModelFolder(artifactFolder);
    Build build = model.getBuild();
    if (build != null) {
      List<Plugin> plugins = build.getPlugins();
      if (plugins != null) {
        return plugins.stream().filter(plugin -> plugin.getArtifactId().equals(MULE_MAVEN_PLUGIN_ARTIFACT_ID)
            && plugin.getGroupId().equals(MULE_MAVEN_PLUGIN_GROUP_ID)).findFirst();

      }
    }
    return empty();
  }

  private void exportSharedLibrariesResourcesAndPackages(Plugin packagingPlugin) {
    doExportSharedLibrariesResourcesAndPackages(packagingPlugin);
  }

  private void processAdditionalPluginLibraries(Plugin packagingPlugin) {
    doProcessAdditionalPluginLibraries(packagingPlugin);
  }

  protected void doProcessAdditionalPluginLibraries(Plugin packagingPlugin) {
    Object configuration = packagingPlugin.getConfiguration();
    if (configuration != null) {
      Xpp3Dom additionalPluginDependenciesDom = ((Xpp3Dom) configuration).getChild(ADDITIONAL_PLUGIN_DEPENDENCIES_FIELD);
      if (additionalPluginDependenciesDom != null) {
        Xpp3Dom[] plugins = additionalPluginDependenciesDom.getChildren(PLUGIN_FIELD);
        if (plugins != null) {
          for (Xpp3Dom plugin : plugins) {
            String pluginGroupId = getAttribute(plugin, GROUP_ID);
            String pluginArtifactId = getAttribute(plugin, ARTIFACT_ID);
            findBundleDependency(pluginGroupId, pluginArtifactId, of(MULE_PLUGIN)).ifPresent(bundleDependency -> {
              Xpp3Dom dependenciesDom = plugin.getChild(PLUGIN_DEPENDENCIES_FIELD);
              if (dependenciesDom != null) {
                Set<BundleDependency> additionalDependencies = new HashSet<>();
                for (Xpp3Dom dependency : dependenciesDom.getChildren(PLUGIN_DEPENDENCY_FIELD)) {
                  String dependencyGroupId = getAttribute(dependency, GROUP_ID);
                  String dependencyArtifactId = getAttribute(dependency, ARTIFACT_ID);
                  String dependencyVersion = getAttribute(dependency, VERSION);
                  additionalDependencies.add(new BundleDependency.Builder()
                      .setDescriptor(new BundleDescriptor.Builder()
                          .setGroupId(dependencyGroupId)
                          .setArtifactId(dependencyArtifactId)
                          .setVersion(dependencyVersion)
                          .build())
                      .build());
                }
                replaceBundleDependency(bundleDependency, new BundleDependency.Builder(bundleDependency)
                    .setAdditionalDependencies(additionalDependencies)
                    .build());
              }
            });
          }
        }
      }
    }

  }

  protected void replaceBundleDependency(BundleDependency original, BundleDependency modified) {
    this.dependencies.remove(original);
    this.dependencies.add(modified);
  }

  /**
   * Template method for exporting shared libraries and packages.
   * By default, the pom needs to be parsed again to find which dependencies need to be shared.
   */
  protected void doExportSharedLibrariesResourcesAndPackages(Plugin packagingPlugin) {
    Object configuration = packagingPlugin.getConfiguration();
    if (configuration != null) {
      Xpp3Dom sharedLibrariesDom = ((Xpp3Dom) configuration).getChild(SHARED_LIBRARIES_FIELD);
      if (sharedLibrariesDom != null) {
        Xpp3Dom[] sharedLibraries = sharedLibrariesDom.getChildren(SHARED_LIBRARY_FIELD);
        if (sharedLibraries != null) {
          for (Xpp3Dom sharedLibrary : sharedLibraries) {
            String groupId = getAttribute(sharedLibrary, GROUP_ID);
            String artifactId = getAttribute(sharedLibrary, ARTIFACT_ID);
            findAndExportSharedLibrary(groupId, artifactId);
          }
        }
      }
    }
  }

  protected String getAttribute(Xpp3Dom tag, String attributeName) {
    Xpp3Dom attributeDom = tag.getChild(attributeName);
    checkState(attributeDom != null, format("'%s' element not declared at '%s' in the pom file of the artifact '%s'",
                                            attributeName, tag.toString(), artifactFolder.getName()));
    String attributeValue = attributeDom.getValue().trim();
    checkState(!isEmpty(attributeValue),
               format("'%s' was defined but has an empty value at '%s' declared in the pom file of the artifact %s",
                      attributeName, tag.toString(), artifactFolder.getName()));
    return attributeValue;

  }

  protected void findAndExportSharedLibrary(String groupId, String artifactId) {
    Optional<BundleDependency> bundleDependencyOptional = findBundleDependency(groupId, artifactId, empty());
    BundleDependency bundleDependency = bundleDependencyOptional.orElseThrow(() -> new MuleRuntimeException(I18nMessageFactory
        .createStaticMessage(format(
                                    "Dependency %s:%s could not be found within the artifact %s. It must be declared within the maven dependencies of the artifact.",
                                    groupId,
                                    artifactId, artifactFolder.getName()))));
    JarInfo jarInfo = fileJarExplorer.explore(bundleDependency.getBundleUri());
    this.exportingPackages(jarInfo.getPackages());
    this.exportingResources(jarInfo.getResources());
  }

  protected Optional<BundleDependency> findBundleDependency(String groupId, String artifactId,
                                                            Optional<String> classifierOptional) {
    return dependencies.stream()
        .filter(bundleDependency -> bundleDependency.getDescriptor().getArtifactId().equals(artifactId)
            && bundleDependency.getDescriptor().getGroupId().equals(groupId)
            && classifierOptional
                .map(classifier -> classifier.equals(bundleDependency.getDescriptor().getClassifier().orElse(null))).orElse(true))
        .findFirst();
  }

  public abstract void includeAdditionPluginDependencies();

}
