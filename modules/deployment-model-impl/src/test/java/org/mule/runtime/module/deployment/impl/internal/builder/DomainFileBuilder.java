/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.builder;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.deployment.meta.Product.MULE;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor.DEFAULT_DEPLOY_PROPERTIES_RESOURCE;
import static org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor.PROPERTY_CONFIG_RESOURCES;
import static org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor.PROPERTY_REDEPLOYMENT_ENABLED;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.deployment.model.api.domain.DomainDescriptor.DEFAULT_CONFIGURATION_RESOURCE;
import static org.mule.runtime.deployment.model.api.domain.DomainDescriptor.MULE_DOMAIN_CLASSIFIER;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.MULE_ARTIFACT_JSON_DESCRIPTOR_LOCATION;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptorBuilder;
import org.mule.runtime.api.deployment.meta.MuleDomainModel;
import org.mule.runtime.api.deployment.persistence.MuleDomainModelJsonSerializer;
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

/**
 * Creates Mule Domain files.
 */
public class DomainFileBuilder extends DeployableFileBuilder<DomainFileBuilder> {

  /**
   * Creates a new builder
   *
   * @param artifactId artifact identifier. Non empty.
   */
  public DomainFileBuilder(String artifactId) {
    super(artifactId);
  }


  /**
   * Creates a new builder from another instance.
   *
   * @param source instance used as template to build the new one. Non null.
   */
  public DomainFileBuilder(DomainFileBuilder source) {
    super(source);
  }

  /**
   * Create a new builder from another instance and different ID.
   *
   * @param artifactId artifact identifier. Non empty.
   * @param source instance used as template to build the new one. Non null.
   */
  public DomainFileBuilder(String artifactId, DomainFileBuilder source) {
    super(artifactId, source);
  }

  @Override
  public String getClassifier() {
    return MULE_DOMAIN_CLASSIFIER;
  }

  @Override
  protected DomainFileBuilder getThis() {
    return this;
  }

  /**
   * Sets the configuration file used for the domain.
   *
   * @param configFile domain configuration from a external file or test resource. Non empty.
   * @return the same builder instance
   */
  public DomainFileBuilder definedBy(String configFile) {
    checkImmutable();
    checkArgument(!StringUtils.isEmpty(configFile), "Config file cannot be empty");
    this.resources.add(new ZipResource(configFile, DEFAULT_CONFIGURATION_RESOURCE));

    return this;
  }

  @Override
  protected List<ZipResource> doGetCustomResources() {
    final List<ZipResource> customResources = new LinkedList<>();

    final ZipResource domainProperties =
        createPropertiesFile(this.deployProperties, DEFAULT_DEPLOY_PROPERTIES_RESOURCE, DEFAULT_DEPLOY_PROPERTIES_RESOURCE);
    if (domainProperties != null) {
      customResources.add(domainProperties);
    }

    Object redeploymentEnabled = deployProperties.get(PROPERTY_REDEPLOYMENT_ENABLED);
    Object configResources = deployProperties.get(PROPERTY_CONFIG_RESOURCES);

    File domainDescriptor = createDomainJsonDescriptorFile(
                                                           redeploymentEnabled == null
                                                               ? empty()
                                                               : ofNullable(Boolean
                                                                   .valueOf((String) redeploymentEnabled)),
                                                           Optional.ofNullable((String) configResources),
                                                           empty());

    customResources.add(new ZipResource(domainDescriptor.getAbsolutePath(), MULE_ARTIFACT_JSON_DESCRIPTOR_LOCATION));
    return customResources;
  }

  @Override
  public String getConfigFile() {
    return DEFAULT_CONFIGURATION_RESOURCE;
  }

  private File createDomainJsonDescriptorFile(Optional<Boolean> redeploymentEnabled,
                                              Optional<String> configResources, Optional<String> exportedResources) {
    File domainDescriptor = new File(getTempFolder(), getArtifactId() + "domain.json");
    domainDescriptor.deleteOnExit();
    MuleDomainModel.MuleDomainModelBuilder muleDomainModelBuilder =
        new MuleDomainModel.MuleDomainModelBuilder();
    muleDomainModelBuilder.setName(getArtifactId()).setMinMuleVersion("4.0.0").setRequiredProduct(MULE);
    redeploymentEnabled.ifPresent(muleDomainModelBuilder::setRedeploymentEnabled);
    configResources.ifPresent(configs -> {
      String[] configFiles = configs.split(",");
      muleDomainModelBuilder.setConfigs(new HashSet<>(asList(configFiles)));
    });
    MuleArtifactLoaderDescriptorBuilder muleArtifactClassLoaderDescriptorBuilder =
        new MuleArtifactLoaderDescriptorBuilder().setId(MULE_LOADER_ID);
    exportedResources.ifPresent(resources -> {
      muleArtifactClassLoaderDescriptorBuilder.addProperty(EXPORTED_RESOURCES, resources.split(","));
    });
    muleDomainModelBuilder.withClassLoaderModelDescriptorLoader(muleArtifactClassLoaderDescriptorBuilder.build());
    muleDomainModelBuilder.withBundleDescriptorLoader(new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()));
    String applicationDescriptorContent = new MuleDomainModelJsonSerializer().serialize(muleDomainModelBuilder.build());
    try (FileWriter fileWriter = new FileWriter(domainDescriptor)) {
      fileWriter.write(applicationDescriptorContent);
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
    return domainDescriptor;
  }
}
