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
import static org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor.MULE_APPLICATION_CLASSIFIER;
import static org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor.MULE_DOMAIN_CLASSIFIER;

import static java.nio.file.Files.find;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;

import org.mule.maven.client.internal.AetherMavenClient;
import org.mule.runtime.api.deployment.meta.MuleDeployableModel;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.maven.model.Model;

public class LightweightDeployableProjectModelBuilder extends AbstractMavenDeployableProjectModelBuilder {

  private final boolean isDomain;
  private final File projectFolder;
  private final Optional<MuleDeployableModel> model;


  public LightweightDeployableProjectModelBuilder(File projectFolder, boolean isDomain) {
    this(projectFolder, empty(), isDomain);
  }

  public LightweightDeployableProjectModelBuilder(File projectFolder, Optional<MuleDeployableModel> model, boolean isDomain) {
    super(getDefaultMavenConfiguration());
    this.projectFolder = projectFolder;
    this.model = model;
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

    List<String> exportedPackages = (List<String>) modelResolver.get()
        .getClassLoaderModelLoaderDescriptor().getAttributes().getOrDefault("exportedPackages", emptyList());

    List<String> exportedResources = (List<String>) modelResolver.get().getClassLoaderModelLoaderDescriptor().getAttributes()
        .getOrDefault("exportedResources", emptyList());

    return new DeployableProjectModel(exportedPackages, exportedResources, emptyList(),
                                      buildBundleDescriptor(deployableArtifactCoordinates, isDomain),
                                      modelResolver,
                                      projectFolder, deployableBundleDependencies,
                                      sharedDeployableBundleDescriptors, additionalPluginDependencies);
  }

  private File getPomFromFolder(File projectFolder) {
    File mavenFolder = new File(projectFolder, "META-INF/maven");
    try (Stream<Path> stream = find(mavenFolder.toPath(), 3, (p, m) -> p.getFileName().toString().equals("pom.xml"))) {
      List<Path> pomLists = stream.collect(toList());
      if (pomLists.size() != 1) {
        throw new MuleRuntimeException(createStaticMessage("Could find the pom in " + mavenFolder.toPath()));
      }
      return pomLists.get(0).toFile();
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
  }

  private Supplier<MuleDeployableModel> getModelResolver() {
    return () -> model.orElseGet(() -> {
      if (isDomain) {
        return domainModelResolver().resolve(new File(projectFolder, "META-INF/mule-artifact"));
      } else {
        return applicationModelResolver().resolve(new File(projectFolder, "META-INF/mule-artifact"));
      }
    });
  }

  private BundleDescriptor buildBundleDescriptor(ArtifactCoordinates artifactCoordinates, boolean isDomain) {
    return new BundleDescriptor.Builder()
      .setArtifactId(artifactCoordinates.getArtifactId())
      .setGroupId(artifactCoordinates.getGroupId())
      .setVersion(artifactCoordinates.getVersion())
      .setBaseVersion(artifactCoordinates.getVersion())
      .setType(artifactCoordinates.getType())
      .setClassifier(isDomain ? MULE_DOMAIN_CLASSIFIER : MULE_APPLICATION_CLASSIFIER)
      .build();
  }

}
