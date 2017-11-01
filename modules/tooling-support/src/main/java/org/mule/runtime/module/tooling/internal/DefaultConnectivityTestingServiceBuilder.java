/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.container.api.MuleFoldersUtil.getExecutionFolder;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenUtils.addSharedLibraryDependency;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenUtils.createDeployablePomFile;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenUtils.updateArtifactPom;
import org.mule.maven.client.api.MavenClientProvider;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.util.UUID;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.globalconfig.api.GlobalConfigLoader;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.deployment.impl.internal.application.DeployableMavenClassLoaderModelLoader;
import org.mule.runtime.module.tooling.api.connectivity.ConnectivityTestingServiceBuilder;

import java.io.File;
import java.util.ArrayList;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

/**
 * Default implementation for {@code ConnectivityTestingServiceBuilder}.
 *
 * @since 4.0
 */
class DefaultConnectivityTestingServiceBuilder implements ConnectivityTestingServiceBuilder {

  private final DefaultApplicationFactory defaultApplicationFactory;
  private ArtifactDeclaration artifactDeclaration;
  private Model model;

  DefaultConnectivityTestingServiceBuilder(DefaultApplicationFactory defaultApplicationFactory) {
    this.defaultApplicationFactory = defaultApplicationFactory;
    createTempMavenModel();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectivityTestingServiceBuilder addDependency(String groupId, String artifactId, String artifactVersion,
                                                         String classifier, String type) {
    Dependency dependency = new Dependency();
    dependency.setGroupId(groupId);
    dependency.setArtifactId(artifactId);
    dependency.setVersion(artifactVersion);
    dependency.setType(type);
    dependency.setClassifier(classifier);
    if (!MULE_PLUGIN_CLASSIFIER.equals(classifier)) {
      addSharedLibraryDependency(model, dependency);
    }
    model.getDependencies().add(dependency);
    return this;
  }

  private void createTempMavenModel() {
    model = new Model();
    model.setArtifactId("temp-artifact-id");
    model.setGroupId("temp-group-id");
    model.setVersion("temp-version");
    model.setDependencies(new ArrayList<>());
    model.setModelVersion("4.0.0");
  }

  /**
   * {@inheritDoc}
   */
  public ConnectivityTestingServiceBuilder setArtifactDeclaration(ArtifactDeclaration artifactDeclaration) {
    this.artifactDeclaration = artifactDeclaration;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectivityTestingService build() {
    checkState(artifactDeclaration != null, "artifact configuration cannot be null");
    return new TemporaryArtifactConnectivityTestingService(() -> {
      String applicationName = UUID.getUUID() + "-connectivity-testing-temp-app";
      File applicationFolder = new File(getExecutionFolder(), applicationName);
      ApplicationDescriptor applicationDescriptor = new ApplicationDescriptor(applicationName);
      applicationDescriptor.setArtifactDeclaration(artifactDeclaration);
      applicationDescriptor.setConfigResources(emptySet());
      applicationDescriptor.setArtifactLocation(applicationFolder);
      createDeployablePomFile(applicationFolder, model);
      updateArtifactPom(applicationFolder, model);
      MavenClientProvider mavenClientProvider =
          MavenClientProvider.discoverProvider(DefaultConnectivityTestingServiceBuilder.class.getClassLoader());
      applicationDescriptor
          .setClassLoaderModel(new DeployableMavenClassLoaderModelLoader(mavenClientProvider
              .createMavenClient(GlobalConfigLoader.getMavenConfig()), mavenClientProvider.getLocalRepositorySuppliers())
                  .load(applicationFolder, emptyMap(), ArtifactType.APP));
      return defaultApplicationFactory.createArtifact(applicationDescriptor);
    });
  }

}
