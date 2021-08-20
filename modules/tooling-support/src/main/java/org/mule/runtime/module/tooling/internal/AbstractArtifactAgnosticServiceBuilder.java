/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.container.api.MuleFoldersUtil.getExecutionFolder;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenUtils.addSharedLibraryDependency;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenUtils.createDeployablePomFile;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenUtils.updateArtifactPom;

import org.mule.maven.client.api.MavenClientProvider;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.util.UUID;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.globalconfig.api.GlobalConfigLoader;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.deployment.impl.internal.application.DeployableMavenClassLoaderModelLoader;
import org.mule.runtime.module.tooling.api.ArtifactAgnosticServiceBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.model.Model;

public abstract class AbstractArtifactAgnosticServiceBuilder<T extends ArtifactAgnosticServiceBuilder, S>
    implements ArtifactAgnosticServiceBuilder<T, S> {

  private static final String TMP_APP_ARTIFACT_ID = "temp-artifact-id";
  private static final String TMP_APP_GROUP_ID = "temp-group-id";
  private static final String TMP_APP_VERSION = "temp-version";
  private static final String TMP_APP_MODEL_VERSION = "4.0.0";

  private final DefaultApplicationFactory defaultApplicationFactory;

  private ArtifactDeclaration artifactDeclaration;
  private Model model;
  private Map<String, String> artifactProperties = emptyMap();

  protected AbstractArtifactAgnosticServiceBuilder(DefaultApplicationFactory defaultApplicationFactory) {
    this.defaultApplicationFactory = defaultApplicationFactory;
    createTempMavenModel();
  }

  @Override
  public T setArtifactProperties(Map<String, String> artifactProperties) {
    checkState(artifactProperties != null, "artifactProperties cannot be null");
    this.artifactProperties = artifactProperties;
    return getThis();
  }

  @Override
  public T setArtifactDeclaration(ArtifactDeclaration artifactDeclaration) {
    checkState(artifactDeclaration != null, "artifactDeclaration cannot be null");
    this.artifactDeclaration = artifactDeclaration;
    return getThis();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T addDependency(String groupId, String artifactId, String artifactVersion,
                         String classifier, String type) {
    org.apache.maven.model.Dependency dependency = new org.apache.maven.model.Dependency();
    dependency.setGroupId(groupId);
    dependency.setArtifactId(artifactId);
    dependency.setVersion(artifactVersion);
    dependency.setType(type);
    dependency.setClassifier(classifier);

    addMavenModelDependency(dependency);
    return getThis();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T addDependency(Dependency dependency) {
    org.apache.maven.model.Dependency mavenModelDependency = new org.apache.maven.model.Dependency();
    mavenModelDependency.setGroupId(dependency.getGroupId());
    mavenModelDependency.setArtifactId(dependency.getArtifactId());
    mavenModelDependency.setVersion(dependency.getVersion());
    mavenModelDependency.setType(dependency.getType());
    mavenModelDependency.setClassifier(dependency.getClassifier());
    mavenModelDependency.setOptional(dependency.getOptional());
    mavenModelDependency.setScope(dependency.getScope());
    mavenModelDependency.setSystemPath(dependency.getSystemPath());
    mavenModelDependency.setExclusions(dependency.getExclusions().stream().map(exclusion -> {
      org.apache.maven.model.Exclusion mavenModelExclusion = new org.apache.maven.model.Exclusion();
      mavenModelExclusion.setGroupId(exclusion.getGroupId());
      mavenModelExclusion.setArtifactId(exclusion.getArtifactId());
      return mavenModelExclusion;
    }).collect(toList()));

    addMavenModelDependency(mavenModelDependency);
    return getThis();
  }

  private void addMavenModelDependency(org.apache.maven.model.Dependency dependency) {
    if (!MULE_PLUGIN_CLASSIFIER.equals(dependency.getClassifier())) {
      addSharedLibraryDependency(model, dependency);
    }
    model.getDependencies().add(dependency);
  }

  @Override
  public S build() {
    checkState(artifactDeclaration != null, "artifact configuration cannot be null");
    return createService(() -> {
      String applicationName = UUID.getUUID() + "-artifact-temp-app";
      File applicationFolder = new File(getExecutionFolder(), applicationName);
      Properties deploymentProperties = new Properties();
      deploymentProperties.putAll(forcedDeploymentProperties());
      ApplicationDescriptor applicationDescriptor = new ApplicationDescriptor(applicationName, of(deploymentProperties));
      applicationDescriptor.setArtifactDeclaration(artifactDeclaration);
      applicationDescriptor.setConfigResources(singleton("empty-app.xml"));
      applicationDescriptor.setArtifactLocation(applicationFolder);
      applicationDescriptor.setAppProperties(artifactProperties);
      createDeployablePomFile(applicationFolder, model);
      updateArtifactPom(applicationFolder, model);
      MavenClientProvider mavenClientProvider =
          MavenClientProvider.discoverProvider(AbstractArtifactAgnosticServiceBuilder.class.getClassLoader());
      applicationDescriptor
          .setClassLoaderModel(new DeployableMavenClassLoaderModelLoader(of(mavenClientProvider
              .createMavenClient(GlobalConfigLoader.getMavenConfig())))
                  .load(applicationFolder, singletonMap(BundleDescriptor.class.getName(),
                                                        createTempBundleDescriptor()),
                        ArtifactType.APP));
      return defaultApplicationFactory.createArtifact(applicationDescriptor);
    });
  }

  protected Map<String, String> forcedDeploymentProperties() {
    return emptyMap();
  }

  protected abstract S createService(ApplicationSupplier applicationSupplier);

  private void createTempMavenModel() {
    model = new Model();
    model.setArtifactId(TMP_APP_ARTIFACT_ID);
    model.setGroupId(TMP_APP_GROUP_ID);
    model.setVersion(TMP_APP_VERSION);
    model.setDependencies(new ArrayList<>());
    model.setModelVersion(TMP_APP_MODEL_VERSION);
  }

  private BundleDescriptor createTempBundleDescriptor() {
    return new BundleDescriptor.Builder().setArtifactId(TMP_APP_ARTIFACT_ID).setGroupId(TMP_APP_GROUP_ID)
        .setVersion(TMP_APP_VERSION).setClassifier("mule-application").build();
  }

  private T getThis() {
    return (T) this;
  }

}
