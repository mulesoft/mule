/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.api.deployment.meta.Product.MULE;
import static org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor.PROPERTY_CONFIG_RESOURCES;
import static org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor.PROPERTY_REDEPLOYMENT_ENABLED;
import static org.mule.runtime.deployment.model.api.application.ApplicationDescriptor.DEFAULT_CONFIGURATION_RESOURCE;
import static org.mule.runtime.deployment.model.api.application.ApplicationDescriptor.MULE_APPLICATION_CLASSIFIER;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.MULE_ARTIFACT_JSON_DESCRIPTOR_LOCATION;

import org.mule.runtime.api.deployment.meta.MuleApplicationModel;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptorBuilder;
import org.mule.runtime.api.deployment.persistence.MuleApplicationModelJsonSerializer;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.tck.ZipUtils.ZipResource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * Creates Mule Application files.
 */
public class ApplicationFileBuilder extends DeployableFileBuilder<ApplicationFileBuilder> {

  private Properties properties = new Properties();

  /**
   * Creates a new builder
   *
   * @param artifactId artifact identifier. Non empty.
   */
  public ApplicationFileBuilder(String artifactId) {
    super(artifactId);
  }

  /**
   * Creates a new builder
   *
   * @param artifactId artifact identifier. Non empty.
   * @param upperCaseInExtension whether the extension is in uppercase
   */
  public ApplicationFileBuilder(String artifactId, boolean upperCaseInExtension) {
    super(artifactId, upperCaseInExtension);
  }


  /**
   * Creates a new builder from another instance.
   *
   * @param source instance used as template to build the new one. Non null.
   */
  public ApplicationFileBuilder(ApplicationFileBuilder source) {
    super(source);
  }

  /**
   * Create a new builder from another instance and different ID.
   *
   * @param id artifact identifier. Non empty.
   * @param source instance used as template to build the new one. Non null.
   */
  public ApplicationFileBuilder(String id, ApplicationFileBuilder source) {
    super(id, source);
    this.properties.putAll(source.properties);
    this.deployProperties.putAll(source.deployProperties);
  }

  @Override
  protected ApplicationFileBuilder getThis() {
    return this;
  }

  /**
   * Sets the configuration file used for the application.
   *
   * @param configFile application configuration from a external file or test resource. Non empty.
   * @return the same builder instance
   */
  public ApplicationFileBuilder definedBy(String configFile) {
    checkImmutable();
    checkArgument(!StringUtils.isEmpty(configFile), "Config file cannot be empty");
    this.resources.add(new ZipResource(configFile, DEFAULT_CONFIGURATION_RESOURCE));

    return this;
  }

  /**
   * Adds a property into the application properties file.
   *
   * @param propertyName name fo the property to add. Non empty
   * @param propertyValue value of the property to add. Non null.
   * @return the same builder instance
   */
  public ApplicationFileBuilder configuredWith(String propertyName, String propertyValue) {
    checkImmutable();
    checkArgument(!StringUtils.isEmpty(propertyName), "Property name cannot be empty");
    checkArgument(propertyValue != null, "Property value cannot be null");
    properties.put(propertyName, propertyValue);
    return this;
  }

  /**
   * Adds a resource file to the artifact folder.
   *
   * @param resourceFile class file from a external file or test resource. Non empty.
   * @param targetFile name to use on the added resource. Non empty.
   * @return the same builder instance
   */
  public ApplicationFileBuilder usingResource(String resourceFile, String targetFile) {
    checkImmutable();
    checkArgument(!isEmpty(resourceFile), "Resource file cannot be empty");
    resources.add(new ZipResource(resourceFile, targetFile));

    return getThis();
  }

  @Override
  public String getConfigFile() {
    return DEFAULT_CONFIGURATION_RESOURCE;
  }

  @Override
  protected List<ZipResource> doGetCustomResources() {
    final List<ZipResource> customResources = new LinkedList<>();

    Object redeploymentEnabled = deployProperties.get(PROPERTY_REDEPLOYMENT_ENABLED);
    Object configResources = deployProperties.get(PROPERTY_CONFIG_RESOURCES);
    File applicationDescriptor = createApplicationJsonDescriptorFile(redeploymentEnabled == null
        ? empty()
        : ofNullable(Boolean
            .valueOf((String) redeploymentEnabled)),
                                                                     Optional.ofNullable((String) configResources),
                                                                     ofNullable((String) properties.get(EXPORTED_PACKAGES)),
                                                                     ofNullable((String) properties.get(EXPORTED_RESOURCES)));
    customResources.add(new ZipResource(applicationDescriptor.getAbsolutePath(), MULE_ARTIFACT_JSON_DESCRIPTOR_LOCATION));
    return customResources;
  }

  private File createApplicationJsonDescriptorFile(Optional<Boolean> redeploymentEnabled, Optional<String> configResources,
                                                   Optional<String> exportedPackages, Optional<String> exportedResources) {
    File applicationDescriptor = new File(getTempFolder(), getArtifactId() + "application.json");
    applicationDescriptor.deleteOnExit();
    MuleApplicationModel.MuleApplicationModelBuilder muleApplicationModelBuilder =
        new MuleApplicationModel.MuleApplicationModelBuilder();
    muleApplicationModelBuilder
        .setName(getArtifactId())
        .setMinMuleVersion("4.0.0")
        .setRequiredProduct(MULE);
    redeploymentEnabled.ifPresent(muleApplicationModelBuilder::setRedeploymentEnabled);
    configResources.ifPresent(configs -> {
      String[] configFiles = configs.split(",");
      muleApplicationModelBuilder.setConfigs(new HashSet<>(asList(configFiles)));
    });
    MuleArtifactLoaderDescriptorBuilder muleArtifactLoaderDescriptorBuilder =
        new MuleArtifactLoaderDescriptorBuilder().setId(MULE_LOADER_ID);

    exportedPackages.ifPresent(packages -> {
      muleArtifactLoaderDescriptorBuilder.addProperty(EXPORTED_PACKAGES, packages.split(","));
    });
    exportedResources.ifPresent(resources -> {
      muleArtifactLoaderDescriptorBuilder.addProperty(EXPORTED_RESOURCES, resources.split(","));
    });

    muleApplicationModelBuilder.withClassLoaderModelDescriptorLoader(muleArtifactLoaderDescriptorBuilder.build());
    muleApplicationModelBuilder.withBundleDescriptorLoader(muleArtifactLoaderDescriptorBuilder.build());
    String applicationDescriptorContent = new MuleApplicationModelJsonSerializer().serialize(muleApplicationModelBuilder.build());
    try (FileWriter fileWriter = new FileWriter(applicationDescriptor)) {
      fileWriter.write(applicationDescriptorContent);
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
    return applicationDescriptor;
  }

  @Override
  public String getClassifier() {
    return MULE_APPLICATION_CLASSIFIER;
  }
}
