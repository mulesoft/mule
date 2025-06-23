/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal;

import static org.mule.maven.pom.parser.api.model.BundleScope.valueOf;
import static org.mule.maven.pom.parser.api.model.MavenModelBuilderProvider.discoverProvider;
import static org.mule.runtime.api.deployment.meta.Product.MULE;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.container.api.MuleFoldersUtil.getExecutionFolder;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.META_INF;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.MULE_ARTIFACT;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;

import static java.nio.file.Files.createDirectories;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.io.IOUtils.copy;

import org.mule.maven.client.api.MavenClient;
import org.mule.maven.client.api.MavenClientProvider;
import org.mule.maven.pom.parser.api.model.ArtifactCoordinates;
import org.mule.maven.pom.parser.api.model.BundleDependency;
import org.mule.maven.pom.parser.api.model.MavenModelBuilder;
import org.mule.maven.pom.parser.api.model.MavenModelBuilderProvider;
import org.mule.runtime.api.artifact.ArtifactType;
import org.mule.runtime.api.deployment.meta.MuleApplicationModel;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.persistence.MuleApplicationModelJsonSerializer;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.core.api.util.UUID;
import org.mule.runtime.globalconfig.api.GlobalConfigLoader;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.deployment.impl.internal.application.DeployableMavenClassLoaderConfigurationLoader;
import org.mule.runtime.module.tooling.api.ArtifactAgnosticServiceBuilder;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.google.common.collect.ImmutableMap;

public abstract class AbstractArtifactAgnosticServiceBuilder<T extends ArtifactAgnosticServiceBuilder, S>
    implements ArtifactAgnosticServiceBuilder<T, S> {

  private static final String TMP_APP_ARTIFACT_ID = "temp-artifact-id";
  private static final String TMP_APP_GROUP_ID = "temp-group-id";
  private static final String TMP_APP_VERSION = "temp-version";
  private static final String TMP_APP_MODEL_VERSION = "4.0.0";

  private final DefaultApplicationFactory defaultApplicationFactory;

  private ArtifactDeclaration artifactDeclaration;

  private MavenModelBuilder model;
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

  protected ArtifactDeclaration getArtifactDeclaration() {
    return artifactDeclaration;
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
    org.mule.maven.pom.parser.api.model.BundleDescriptor bundleDescriptor =
        new org.mule.maven.pom.parser.api.model.BundleDescriptor.Builder()
            .setGroupId(groupId)
            .setArtifactId(artifactId)
            .setVersion(artifactVersion)
            .setType(type)
            .setClassifier(classifier).build();

    BundleDependency bundleDependency = new BundleDependency.Builder().setBundleDescriptor(bundleDescriptor).build();

    addMavenModelDependency(bundleDependency);
    return getThis();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T addDependency(Dependency dependency) {
    org.mule.maven.pom.parser.api.model.BundleDescriptor bundleDescriptor =
        new org.mule.maven.pom.parser.api.model.BundleDescriptor.Builder()
            .setGroupId(dependency.getGroupId())
            .setArtifactId(dependency.getArtifactId())
            .setVersion(dependency.getVersion())
            .setType(dependency.getType())
            .setClassifier(dependency.getClassifier())
            .setOptional(dependency.getOptional())
            .setSystemPath(dependency.getSystemPath())
            .setExclusions(dependency.getExclusions().stream()
                .map(exclusion -> new ArtifactCoordinates(exclusion.getGroupId(), exclusion.getArtifactId())).collect(toList()))
            .build();


    BundleDependency.Builder bundleDependencyBuilder = new BundleDependency.Builder().setBundleDescriptor(bundleDescriptor);
    if (dependency.getScope() != null) {
      bundleDependencyBuilder.setScope(valueOf(dependency.getScope().toUpperCase())).build();
    }

    addMavenModelDependency(bundleDependencyBuilder.build());
    return getThis();
  }

  private void addMavenModelDependency(BundleDependency bundleDependency) {
    org.mule.maven.pom.parser.api.model.BundleDescriptor descriptor = bundleDependency.getDescriptor();
    if (!MULE_PLUGIN_CLASSIFIER.equals(descriptor.getClassifier().orElse(null))) {
      model.addSharedLibraryDependency(descriptor.getGroupId(), descriptor.getArtifactId());
    }
    model.addDependency(bundleDependency);
  }

  @Override
  public S build() {
    checkState(artifactDeclaration != null, "artifact configuration cannot be null");
    return createService(() -> {
      String applicationName = UUID.getUUID() + "-artifact-temp-app";
      File applicationFolder = new File(getExecutionFolder(), applicationName);
      Properties deploymentProperties = new Properties();
      deploymentProperties.putAll(forcedDeploymentProperties());
      Set<String> configs = singleton("empty-app.xml");

      model.createDeployablePomFile(applicationFolder.toPath());
      model.updateArtifactPom(applicationFolder.toPath());

      MavenClientProvider mavenClientProvider =
          MavenClientProvider.discoverProvider(AbstractArtifactAgnosticServiceBuilder.class.getClassLoader());
      ClassLoaderConfiguration classLoaderConfiguration;
      try (MavenClient mavenClient = mavenClientProvider.createMavenClient(GlobalConfigLoader.getMavenConfig())) {
        classLoaderConfiguration =
            new DeployableMavenClassLoaderConfigurationLoader(of(mavenClient))
                .load(applicationFolder, singletonMap(BundleDescriptor.class.getName(),
                                                      createTempBundleDescriptor()),
                      ArtifactType.APP);
      }

      for (String config : configs) {
        copy(this.getClass().getClassLoader().getResource(config), new File(applicationFolder, config));
      }

      File destinationFolder =
          applicationFolder.toPath().resolve(META_INF).resolve(MULE_ARTIFACT).toFile();
      createDirectories(destinationFolder.toPath());

      MuleVersion muleVersion = new MuleVersion("4.4.0");
      String artifactJson =
          new MuleApplicationModelJsonSerializer().serialize(serializeModel(applicationName, classLoaderConfiguration,
                                                                            configs,
                                                                            muleVersion.toCompleteNumericVersion()));

      try (FileWriter fileWriter = new FileWriter(new File(destinationFolder, "mule-artifact.json"))) {
        fileWriter.write(artifactJson);
      }

      ApplicationDescriptor artifactDescriptor =
          defaultApplicationFactory.createArtifactDescriptor(applicationFolder, of(deploymentProperties));
      artifactDescriptor.setMinMuleVersion(muleVersion);
      artifactDescriptor.setAppProperties(artifactProperties);
      return defaultApplicationFactory.createArtifact(artifactDescriptor);
    });
  }

  private MuleApplicationModel serializeModel(String appName,
                                              ClassLoaderConfiguration classLoaderConfiguration,
                                              Set<String> configs, String muleVersion) {
    Map<String, Object> attributes = ImmutableMap.of("exportedResources",
                                                     newArrayList(classLoaderConfiguration.getExportedResources()),
                                                     "exportedPackages",
                                                     newArrayList(classLoaderConfiguration.getExportedPackages()));
    MuleArtifactLoaderDescriptor muleArtifactLoaderDescriptor = new MuleArtifactLoaderDescriptor("mule", attributes);
    MuleApplicationModel.MuleApplicationModelBuilder builder = new MuleApplicationModel.MuleApplicationModelBuilder();
    builder.setName(appName)
        .setMinMuleVersion(muleVersion)
        .setRequiredProduct(MULE)
        .withBundleDescriptorLoader(new MuleArtifactLoaderDescriptor("mule", emptyMap()))
        .withClassLoaderModelDescriptorLoader(muleArtifactLoaderDescriptor)
        .setConfigs(configs);
    return builder.build();
  }



  protected Map<String, String> forcedDeploymentProperties() {
    return emptyMap();
  }

  protected abstract S createService(ApplicationSupplier applicationSupplier);

  private void createTempMavenModel() {
    MavenModelBuilderProvider mavenModelBuilderProvider = discoverProvider();
    model = mavenModelBuilderProvider
        .createMavenModelBuilder(TMP_APP_GROUP_ID, TMP_APP_ARTIFACT_ID, "4.4.0", of(TMP_APP_MODEL_VERSION), empty());
  }

  private BundleDescriptor createTempBundleDescriptor() {
    return new BundleDescriptor.Builder().setArtifactId(TMP_APP_ARTIFACT_ID).setGroupId(TMP_APP_GROUP_ID)
        .setVersion(TMP_APP_VERSION).setClassifier("mule-application").build();
  }

  private T getThis() {
    return (T) this;
  }

  // Used in test cases
  MavenModelBuilder getModel() {
    return model;
  }

  // Used in test cases
  void setModel(MavenModelBuilder model) {
    this.model = model;
  }

}
