/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.DOMAIN;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.INCLUDE_TEST_DEPENDENCIES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.tools.api.classloader.AppClassLoaderModelJsonSerializer.deserialize;

import org.mule.maven.client.api.MavenClient;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.artifact.internal.util.JarExplorer;
import org.mule.runtime.module.deployment.impl.internal.maven.AbstractMavenClassLoaderConfigurationLoader;
import org.mule.runtime.module.deployment.impl.internal.maven.ArtifactClassLoaderConfigurationBuilder;
import org.mule.runtime.module.deployment.impl.internal.maven.HeavyweightClassLoaderConfigurationBuilder;
import org.mule.runtime.module.deployment.impl.internal.maven.LightweightClassLoaderConfigurationBuilder;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible of returning the {@link BundleDescriptor} of a given plugin's location and also creating a
 * {@link ClassLoaderConfiguration}
 *
 * @since 4.0
 */
public class DeployableMavenClassLoaderConfigurationLoader extends AbstractMavenClassLoaderConfigurationLoader {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  public DeployableMavenClassLoaderConfigurationLoader(Optional<MavenClient> mavenClient) {
    super(mavenClient);
  }

  public DeployableMavenClassLoaderConfigurationLoader(Optional<MavenClient> mavenClient,
                                                       Supplier<JarExplorer> jarExplorerFactory) {
    super(mavenClient, jarExplorerFactory);
  }

  @Override
  public String getId() {
    return MULE_LOADER_ID;
  }

  @Override
  protected LightweightClassLoaderConfigurationBuilder newLightweightClassLoaderConfigurationBuilder(File artifactFile,
                                                                                                     BundleDescriptor artifactBundleDescriptor,
                                                                                                     MavenClient mavenClient,
                                                                                                     Map<String, Object> attributes,
                                                                                                     List<BundleDependency> nonProvidedDependencies) {
    return new LightweightClassLoaderConfigurationBuilder(artifactFile, artifactBundleDescriptor, mavenClient,
                                                          nonProvidedDependencies);
  }

  @Override
  protected HeavyweightClassLoaderConfigurationBuilder newHeavyWeightClassLoaderConfigurationBuilder(File artifactFile,
                                                                                                     BundleDescriptor artifactBundleDescriptor,
                                                                                                     org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel,
                                                                                                     Map<String, Object> attributes) {
    return new HeavyweightClassLoaderConfigurationBuilder(artifactFile, artifactBundleDescriptor, packagerClassLoaderModel);
  }

  @Override
  protected List<URL> addArtifactSpecificClassloaderConfiguration(ArtifactClassLoaderConfigurationBuilder classLoaderConfigurationBuilder) {
    classLoaderConfigurationBuilder.exportingSharedLibraries();
    classLoaderConfigurationBuilder.additionalPluginLibraries();

    return super.addArtifactSpecificClassloaderConfiguration(classLoaderConfigurationBuilder);
  }

  @Override
  protected boolean includeTestDependencies(Map<String, Object> attributes) {
    return Boolean.valueOf((String) attributes.getOrDefault(INCLUDE_TEST_DEPENDENCIES, "false"));
  }

  @Override
  protected boolean includeProvidedDependencies(ArtifactType artifactType) {
    return supportsArtifactType(artifactType);
  }

  @Override
  public boolean supportsArtifactType(ArtifactType artifactType) {
    return artifactType.equals(APP) || artifactType.equals(DOMAIN) || artifactType.equals(POLICY);
  }

  @Override
  protected org.mule.tools.api.classloader.model.ClassLoaderModel getPackagerClassLoaderModel(File classLoaderModelDescriptor) {
    return deserialize(classLoaderModelDescriptor);
  }
}
