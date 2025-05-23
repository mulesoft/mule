/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.plugin;

import static org.mule.runtime.api.artifact.ArtifactType.PLUGIN;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.internal.util.jar.JarLoadingUtils.loadFileContentFrom;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.MULE_ARTIFACT_JSON_DESCRIPTOR;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor.MULE_ARTIFACT_PATH_INSIDE_JAR;

import static java.lang.String.format;

import org.mule.runtime.api.artifact.ArtifactType;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.deployment.persistence.AbstractMuleArtifactModelJsonSerializer;
import org.mule.runtime.api.deployment.persistence.MulePluginModelJsonSerializer;
import org.mule.runtime.deployment.model.api.artifact.DescriptorLoaderRepositoryFactory;
import org.mule.runtime.module.artifact.api.descriptor.AbstractArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfigurationLoader;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;
import org.mule.runtime.module.artifact.api.plugin.LoaderDescriber;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Creates {@link ArtifactPluginDescriptor} instances
 */
public class ArtifactPluginDescriptorFactory
    extends AbstractArtifactDescriptorFactory<MulePluginModel, ArtifactPluginDescriptor> {

  /**
   * Creates a default factory
   */
  public ArtifactPluginDescriptorFactory() {
    this(new DescriptorLoaderRepositoryFactory().createDescriptorLoaderRepository(),
         ArtifactDescriptorValidatorBuilder.builder());
  }

  /**
   * Creates a custom factory
   *
   * @param descriptorLoaderRepository         contains all the {@link ClassLoaderConfigurationLoader} registered on the
   *                                           container. Non null
   * @param artifactDescriptorValidatorBuilder {@link ArtifactDescriptorValidatorBuilder} builder to define the validator to be
   *                                           used. Non null.
   */
  public ArtifactPluginDescriptorFactory(DescriptorLoaderRepository descriptorLoaderRepository,
                                         ArtifactDescriptorValidatorBuilder artifactDescriptorValidatorBuilder) {
    super(descriptorLoaderRepository, artifactDescriptorValidatorBuilder);
  }

  @Override
  public ArtifactPluginDescriptor create(File pluginJarFile, Optional<Properties> deploymentProperties)
      throws ArtifactDescriptorCreateException {
    try {
      checkArgument(pluginJarFile.isDirectory() || pluginJarFile.getName().endsWith(".jar"),
                    "provided file is not a plugin: " + pluginJarFile.getAbsolutePath());
      // Use / instead of File.separator as the file is going to be accessed inside the jar as a URL
      String mulePluginJsonPathInsideJarFile = MULE_ARTIFACT_PATH_INSIDE_JAR + "/" + MULE_ARTIFACT_JSON_DESCRIPTOR;
      Optional<byte[]> jsonDescriptorContentOptional = loadFileContentFrom(pluginJarFile, mulePluginJsonPathInsideJarFile);
      return jsonDescriptorContentOptional
          .map(jsonDescriptorContent -> loadFromJsonDescriptor(pluginJarFile,
                                                               loadModelFromJson(new String(jsonDescriptorContent)),
                                                               deploymentProperties))
          .orElseThrow(() -> new ArtifactDescriptorCreateException(pluginDescriptorNotFound(pluginJarFile,
                                                                                            mulePluginJsonPathInsideJarFile)));
    } catch (ArtifactDescriptorCreateException e) {
      throw e;
    } catch (IOException e) {
      throw new ArtifactDescriptorCreateException(e);
    }
  }

  @Override
  protected Map<String, Object> getClassLoaderConfigurationAttributes(Optional<Properties> deploymentPropertiesOptional,
                                                                      MuleArtifactLoaderDescriptor classLoaderModelLoaderDescriptor,
                                                                      BundleDescriptor bundleDescriptor) {
    Map<String, Object> attributes =
        super.getClassLoaderConfigurationAttributes(deploymentPropertiesOptional, classLoaderModelLoaderDescriptor,
                                                    bundleDescriptor);

    if (deploymentPropertiesOptional.isPresent()) {
      Properties deploymentProperties = deploymentPropertiesOptional.get();
      if (deploymentProperties instanceof PluginExtendedDeploymentProperties) {
        PluginExtendedDeploymentProperties pluginExtendedDeploymentProperties =
            (PluginExtendedDeploymentProperties) deploymentProperties;
        return new PluginExtendedClassLoaderConfigurationAttributes(attributes,
                                                                    pluginExtendedDeploymentProperties
                                                                        .getDeployableArtifactDescriptor());
      }
    }
    return attributes;
  }

  @Override
  protected Map<String, Object> getBundleDescriptorAttributes(MuleArtifactLoaderDescriptor bundleDescriptorLoader,
                                                              Optional<Properties> deploymentPropertiesOptional) {
    Map<String, Object> attributes =
        super.getBundleDescriptorAttributes(bundleDescriptorLoader, deploymentPropertiesOptional);

    if (deploymentPropertiesOptional.isPresent()) {
      Properties deploymentProperties = deploymentPropertiesOptional.get();
      if (deploymentProperties instanceof PluginExtendedDeploymentProperties) {
        PluginExtendedDeploymentProperties pluginExtendedDeploymentProperties =
            (PluginExtendedDeploymentProperties) deploymentProperties;
        return new PluginExtendedBundleDescriptorAttributes(attributes,
                                                            pluginExtendedDeploymentProperties.getPluginBundleDescriptor());
      }
    }
    return attributes;
  }

  @Override
  protected ArtifactType getArtifactType() {
    return PLUGIN;
  }

  @Override
  protected void doDescriptorConfig(MulePluginModel artifactModel, ArtifactPluginDescriptor descriptor, File artifactLocation) {
    artifactModel.getExtensionModelLoaderDescriptor().ifPresent(extensionModelDescriptor -> {
      final LoaderDescriber loaderDescriber = new LoaderDescriber(extensionModelDescriptor.getId());
      loaderDescriber.addAttributes(extensionModelDescriptor.getAttributes());
      descriptor.setExtensionModelDescriptorProperty(loaderDescriber);
    });
    artifactModel.getLicense().ifPresent(descriptor::setLicenseModel);
  }

  @Override
  protected ArtifactPluginDescriptor createArtifactDescriptor(File artifactLocation, String name,
                                                              Optional<Properties> deploymentProperties) {
    return new ArtifactPluginDescriptor(name);
  }

  private static String pluginDescriptorNotFound(File pluginFile, String mulePluginJsonPathInsideJarFile) {
    return format("The plugin descriptor '%s' on plugin file '%s' is not present", mulePluginJsonPathInsideJarFile, pluginFile);
  }

  @Override
  protected AbstractMuleArtifactModelJsonSerializer<MulePluginModel> getMuleArtifactModelJsonSerializer() {
    return new MulePluginModelJsonSerializer();
  }

}
