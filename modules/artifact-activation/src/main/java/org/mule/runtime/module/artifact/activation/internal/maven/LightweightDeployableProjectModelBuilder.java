/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.maven;

import static org.mule.maven.client.internal.util.MavenUtils.getPomModelFromFile;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.artifact.activation.api.deployable.ArtifactModelResolver.applicationModelResolver;
import static org.mule.runtime.module.artifact.activation.api.deployable.ArtifactModelResolver.domainModelResolver;

import static java.nio.file.Files.find;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import org.mule.maven.client.internal.AetherMavenClient;
import org.mule.runtime.api.deployment.meta.MuleDeployableModel;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.maven.model.Model;

public class LightweightDeployableProjectModelBuilder extends MavenDeployableProjectModelBuilder {

  private final boolean isDomain;

  public LightweightDeployableProjectModelBuilder(File projectFolder, boolean isDomain) {
    super(projectFolder);
    this.isDomain = isDomain;
  }

  @Override
  public DeployableProjectModel build() {
    File pom = getPomFromFolder(projectFolder);
    Model pomModel = getPomModelFromFile(pom);

    deployableArtifactRepositoryFolder = this.mavenConfiguration.getLocalMavenRepositoryLocation();

    ArtifactCoordinates deployableArtifactCoordinates = getDeployableProjectArtifactCoordinates(pomModel);

    AetherMavenClient aetherMavenClient = new AetherMavenClient(mavenConfiguration);
    List<String> activeProfiles = mavenConfiguration.getActiveProfiles().orElse(emptyList());

    resolveDeployableDependencies(aetherMavenClient, pom, pomModel, activeProfiles);

    resolveDeployablePluginsData(deployableMavenBundleDependencies);

    resolveAdditionalPluginDependencies(aetherMavenClient, pomModel, activeProfiles, pluginsArtifactDependencies);

    Supplier<MuleDeployableModel> modelResolver = getModelResolver();

    List<String> exportedPackages =
        (List<String>) modelResolver.get().getClassLoaderModelLoaderDescriptor().getAttributes().get("exportedPackages");

    List<String> exportedResources =
        (List<String>) modelResolver.get().getClassLoaderModelLoaderDescriptor().getAttributes().get("exportedResources");

    return new DeployableProjectModel(exportedPackages, exportedResources, emptyList(),
                                      buildBundleDescriptor(deployableArtifactCoordinates),
                                      modelResolver,
                                      projectFolder, deployableBundleDependencies,
                                      sharedDeployableBundleDescriptors, additionalPluginDependencies);
  }

  private File getPomFromFolder(File projectFolder) {
    File mavenFolder = new File(projectFolder, "META-INF/maven");
    try (Stream<Path> stream = find(mavenFolder.toPath(), 3, (p, m) -> p.getFileName().toString().equals("pom.xml"))) {
      List<Path> pomLists = stream.collect(toList());
      if (pomLists.size() != 1) {
        throw new MuleRuntimeException(createStaticMessage(""));
      }
      return pomLists.get(0).toFile();
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
  }

  private Supplier<MuleDeployableModel> getModelResolver() {
    if (isDomain) {
      return () -> domainModelResolver().resolve(new File(projectFolder, "META-INF/mule-artifact"));
    } else {
      return () -> applicationModelResolver().resolve(new File(projectFolder, "META-INF/mule-artifact"));
    }
  }

}
