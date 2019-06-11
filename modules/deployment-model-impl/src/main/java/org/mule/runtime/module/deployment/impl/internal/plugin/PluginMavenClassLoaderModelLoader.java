/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.plugin;

import static java.nio.file.Paths.get;
import static java.util.Optional.empty;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.PLUGIN;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.META_INF;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.MULE_ARTIFACT;
import static org.mule.tools.api.classloader.ClassLoaderModelJsonSerializer.deserialize;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mule.maven.client.api.MavenClient;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.api.descriptor.InvalidDescriptorLoaderException;
import org.mule.runtime.module.deployment.impl.internal.maven.AbstractMavenClassLoaderModelLoader;
import org.mule.runtime.module.deployment.impl.internal.maven.ArtifactClassLoaderModelBuilder;
import org.mule.runtime.module.deployment.impl.internal.maven.HeavyweightClassLoaderModelBuilder;
import org.mule.runtime.module.deployment.impl.internal.maven.LightweightClassLoaderModelBuilder;
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
  protected ClassLoaderModel createClassLoaderModel(File artifactFile, Map<String, Object> attributes, ArtifactType artifactType)
      throws InvalidDescriptorLoaderException {
    if (super.isHeavyPackage(artifactFile, attributes)) {
      return super.createClassLoaderModel(artifactFile, attributes, artifactType);
    }

    // If it is a lightweight which uses the local repository a class-loader-model.json may be present in the
    // META-INF/mule-artifact
    if (attributes instanceof PluginExtendedClassLoaderModelAttributes) {
      PluginExtendedClassLoaderModelAttributes pluginExtendedClassLoaderModelAttributes =
          (PluginExtendedClassLoaderModelAttributes) attributes;
      BundleDescriptor pluginBundleDescriptor =
          (BundleDescriptor) pluginExtendedClassLoaderModelAttributes.get(BundleDescriptor.class.getName());
      File rootFolder = pluginExtendedClassLoaderModelAttributes.getDeployableArtifactDescriptor().getRootFolder();
      Path muleArtifactJson =
          get(rootFolder.getAbsolutePath(), META_INF, MULE_ARTIFACT, pluginBundleDescriptor.getGroupId(),
              pluginBundleDescriptor.getArtifactId(), pluginBundleDescriptor.getBaseVersion(),
              CLASSLOADER_MODEL_JSON_DESCRIPTOR);
      if (muleArtifactJson.toFile().exists()) {
        return createHeavyPackageClassLoaderModel(artifactFile, muleArtifactJson.toFile(), attributes, empty());
      }
    }
    return createLightPackageClassLoaderModel(artifactFile, attributes, artifactType);
  }

  @Override
  public String getId() {
    return MULE_LOADER_ID;
  }

  @Override
  protected List<URL> addArtifactSpecificClassloaderConfiguration(ArtifactClassLoaderModelBuilder classLoaderModelBuilder) {
    return classLoaderModelBuilder.includeAdditionalPluginDependencies();
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
