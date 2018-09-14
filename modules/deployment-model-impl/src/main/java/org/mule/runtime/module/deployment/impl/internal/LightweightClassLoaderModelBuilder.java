/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.module.artifact.api.classloader.MuleMavenPlugin.MULE_MAVEN_PLUGIN_ARTIFACT_ID;
import static org.mule.runtime.module.artifact.api.classloader.MuleMavenPlugin.MULE_MAVEN_PLUGIN_GROUP_ID;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenUtils.getPomModelFolder;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.internal.util.FileJarExplorer;
import org.mule.runtime.module.artifact.internal.util.JarInfo;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class LightweightClassLoaderModelBuilder extends ClassLoaderModel.ClassLoaderModelBuilder {

  protected Set<BundleDependency> bundleDependencies;

  private boolean processSharedLibraries = false;
  private File applicationFolder;
  private FileJarExplorer fileJarExplorer = new FileJarExplorer();

  public LightweightClassLoaderModelBuilder(File applicationFolder) {
    this.applicationFolder = applicationFolder;
  }

  @Override
  public ClassLoaderModel.ClassLoaderModelBuilder dependingOn(Set<BundleDependency> dependencies) {
    super.dependingOn(dependencies);
    this.bundleDependencies = dependencies;
    return this;
  }

  public ClassLoaderModel.ClassLoaderModelBuilder sharingLibraries() {
    this.processSharedLibraries = true;
    return this;
  }

  @Override
  public ClassLoaderModel build() {
    if (processSharedLibraries) {
      exportSharedLibrariesResourcesAndPackages();
    }
    return super.build();
  }

  private void exportSharedLibrariesResourcesAndPackages() {
    doExportSharedLibrariesResourcesAndPackages();
  }

  /**
   * Template method for exporting shared libraries and packages. If no classloader-model.json is provided or
   * the information needed is not present, the pom needs to be parsed again to find which dependencies need to be shared.
   */
  protected void doExportSharedLibrariesResourcesAndPackages() {
    Model model = getPomModelFolder(applicationFolder);
    Build build = model.getBuild();
    if (build != null) {
      List<Plugin> plugins = build.getPlugins();
      if (plugins != null) {
        Optional<Plugin> packagingPluginOptional =
            plugins.stream().filter(plugin -> plugin.getArtifactId().equals(MULE_MAVEN_PLUGIN_ARTIFACT_ID)
                && plugin.getGroupId().equals(MULE_MAVEN_PLUGIN_GROUP_ID)).findFirst();
        packagingPluginOptional.ifPresent(packagingPlugin -> {
          Object configuration = packagingPlugin.getConfiguration();
          if (configuration != null) {
            Xpp3Dom sharedLibrariesDom = ((Xpp3Dom) configuration).getChild("sharedLibraries");
            if (sharedLibrariesDom != null) {
              Xpp3Dom[] sharedLibraries = sharedLibrariesDom.getChildren("sharedLibrary");
              if (sharedLibraries != null) {
                for (Xpp3Dom sharedLibrary : sharedLibraries) {
                  String groupId = getSharedLibraryAttribute(applicationFolder, sharedLibrary, "groupId");
                  String artifactId = getSharedLibraryAttribute(applicationFolder, sharedLibrary, "artifactId");
                  findAndExportSharedLibrary(groupId, artifactId);
                }
              }
            }
          }
        });
      }
    }
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

  protected void findAndExportSharedLibrary(String groupId, String artifactId) {
    Optional<BundleDependency> bundleDependencyOptional = bundleDependencies.stream()
        .filter(bundleDependency -> bundleDependency.getDescriptor().getArtifactId().equals(artifactId)
            && bundleDependency.getDescriptor().getGroupId().equals(groupId))
        .findFirst();
    bundleDependencyOptional.map(bundleDependency -> {
      JarInfo jarInfo = fileJarExplorer.explore(bundleDependency.getBundleUri());
      this.exportingPackages(jarInfo.getPackages());
      this.exportingResources(jarInfo.getResources());
      return bundleDependency;
    }).orElseThrow(
                   () -> new MuleRuntimeException(I18nMessageFactory
                       .createStaticMessage(format(
                                                   "Dependency %s:%s could not be found within the artifact %s. It must be declared within the maven dependencies of the artifact.",
                                                   groupId,
                                                   artifactId, applicationFolder.getName()))));
  }

}
