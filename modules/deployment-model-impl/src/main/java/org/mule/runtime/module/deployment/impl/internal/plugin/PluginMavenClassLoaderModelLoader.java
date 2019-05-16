/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.plugin;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.PLUGIN;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.tools.api.classloader.ClassLoaderModelJsonSerializer.deserialize;
import org.mule.maven.client.api.MavenClient;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.deployment.impl.internal.maven.AbstractMavenClassLoaderModelLoader;
import org.mule.runtime.module.deployment.impl.internal.maven.ArtifactClassLoaderModelBuilder;
import org.mule.runtime.module.deployment.impl.internal.maven.HeavyweightClassLoaderModelBuilder;
import org.mule.runtime.module.deployment.impl.internal.maven.LightweightClassLoaderModelBuilder;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible of returning the {@link BundleDescriptor} of a given plugin's location and also creating a
 * {@link ClassLoaderModel}
 *
 * @since 4.0
 */
public class PluginMavenClassLoaderModelLoader extends AbstractMavenClassLoaderModelLoader {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  public PluginMavenClassLoaderModelLoader(MavenClient mavenClient) {
    super(mavenClient);
  }

  @Override
  public String getId() {
    return MULE_LOADER_ID;
  }

  @Override
  protected void addArtifactSpecificClassloaderConfiguration(ArtifactClassLoaderModelBuilder classLoaderModelBuilder) {
    classLoaderModelBuilder.includeAdditionalPluginDependencies();
  }

  @Override
  public boolean supportsArtifactType(ArtifactType artifactType) {
    return artifactType.equals(PLUGIN);
  }

  @Override
  protected boolean includeProvidedDependencies(ArtifactType artifactType) {
    return false;
  }

  @Override
  protected File getClassLoaderModelDescriptor(File artifactFile) {
    return new File(artifactFile.getParent(), CLASSLOADER_MODEL_JSON_DESCRIPTOR);
  }

  @Override
  protected LightweightClassLoaderModelBuilder newLightweightClassLoaderModelBuilder(File artifactFile,
                                                                                     BundleDescriptor artifactBundleDescriptor,
                                                                                     MavenClient mavenClient,
                                                                                     Map<String, Object> attributes,
                                                                                     Set<BundleDependency> nonProvidedDependencies) {
    final LightweightClassLoaderModelBuilder lightweightClassLoaderModelBuilder =
        new LightweightClassLoaderModelBuilder(artifactFile, artifactBundleDescriptor, mavenClient, nonProvidedDependencies);
    configClassLoaderModelBuilder(lightweightClassLoaderModelBuilder, attributes);
    return lightweightClassLoaderModelBuilder;
  }

  @Override
  protected HeavyweightClassLoaderModelBuilder newHeavyWeightClassLoaderModelBuilder(File artifactFile,
                                                                                     BundleDescriptor artifactBundleDescriptor,
                                                                                     org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel,
                                                                                     Map<String, Object> attributes) {
    final HeavyweightClassLoaderModelBuilder heavyweightClassLoaderModelBuilder =
        new HeavyweightClassLoaderModelBuilder(artifactFile, artifactBundleDescriptor, packagerClassLoaderModel);
    configClassLoaderModelBuilder(heavyweightClassLoaderModelBuilder, attributes);
    return heavyweightClassLoaderModelBuilder;
  }

  @Override
  protected Set<BundleDependency> resolveArtifactDependencies(File artifactFile, Map<String, Object> attributes,
                                                              ArtifactType artifactType) {
    if(attributes instanceof PluginExtendedClassLoaderModelAttributes) {
      BundleDescriptor pluginBundleDescriptor = (BundleDescriptor) attributes.get(BundleDescriptor.class.getName());
      ArtifactDescriptor deployableArtifactDescriptor = ((PluginExtendedClassLoaderModelAttributes) attributes).getDeployableArtifactDescriptor();
      Set<BundleDependency> deployableArtifactDescriptorDependencies = deployableArtifactDescriptor.getClassLoaderModel().getDependencies();
      BundleDependency pluginDependencyInDeployableArtifact = deployableArtifactDescriptorDependencies.stream().filter(dep -> dep.getDescriptor().equals(pluginBundleDescriptor)).findFirst().orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Could not find required descriptor. Looking for: " + pluginBundleDescriptor + " in " + deployableArtifactDescriptorDependencies)));
      return collectTransitiveDependencies(pluginDependencyInDeployableArtifact);
    }
    return super.resolveArtifactDependencies(artifactFile, attributes, artifactType);
  }

  private Set<BundleDependency> collectTransitiveDependencies(BundleDependency rootDependency) {
    Set<BundleDependency> allTransitiveDependencies = new HashSet<>();
    for(BundleDependency transitiveDependency : rootDependency.getTransitiveDependencies()) {
      allTransitiveDependencies.add(transitiveDependency);
      allTransitiveDependencies.addAll(collectTransitiveDependencies(transitiveDependency));
    }
    return allTransitiveDependencies;
  }

  private void configClassLoaderModelBuilder(ArtifactClassLoaderModelBuilder classLoaderModelBuilder,
                                             Map<String, Object> attributes) {
    if (attributes instanceof PluginExtendedClassLoaderModelAttributes) {
      classLoaderModelBuilder.setDeployableArtifactDescriptor(((PluginExtendedClassLoaderModelAttributes) attributes)
          .getDeployableArtifactDescriptor());
    }
  }

  @Override
  protected org.mule.tools.api.classloader.model.ClassLoaderModel getPackagerClassLoaderModel(File classLoaderModelDescriptor) {
    return deserialize(classLoaderModelDescriptor);
  }
}
