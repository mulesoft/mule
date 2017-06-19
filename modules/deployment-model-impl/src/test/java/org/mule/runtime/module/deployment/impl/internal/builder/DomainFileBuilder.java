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
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor.DEFAULT_DEPLOY_PROPERTIES_RESOURCE;
import static org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor.PROPERTY_CONFIG_RESOURCES;
import static org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor.PROPERTY_REDEPLOYMENT_ENABLED;
import static org.mule.runtime.deployment.model.api.domain.DomainDescriptor.DEFAULT_CONFIGURATION_RESOURCE;
import static org.mule.runtime.deployment.model.api.domain.DomainDescriptor.MULE_DOMAIN_JSON_LOCATION;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.MAVEN;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleDomainModel;
import org.mule.runtime.api.deployment.persistence.MuleDomainModelJsonSerializer;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.tck.ZipUtils.ZipResource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * Creates Mule Domain files.
 */
public class DomainFileBuilder extends DeployableFileBuilder<DomainFileBuilder> {

  private List<ApplicationFileBuilder> applications = new LinkedList<>();

  /**
   * Creates a new builder
   *
   * @param id artifact identifier. Non empty.
   */
  public DomainFileBuilder(String id) {
    super(id);
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
   * @param id artifact identifier. Non empty.
   * @param source instance used as template to build the new one. Non null.
   */
  public DomainFileBuilder(String id, DomainFileBuilder source) {
    super(id, source);
    this.applications.addAll(source.applications);
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
    this.resources.add(new ZipResource(configFile, "mule" + File.separator + DEFAULT_CONFIGURATION_RESOURCE));

    return this;
  }

  /**
   * Adds an application to the domain.
   *
   * @param appFileBuilder builder defining the application. Non null.
   * @return the same builder instance
   */
  public DomainFileBuilder containing(ApplicationFileBuilder appFileBuilder) {
    checkImmutable();
    checkArgument(appFileBuilder != null, "Application builder cannot be null");
    this.applications.add(appFileBuilder);

    return this;
  }

  @Override
  protected List<ZipResource> doGetCustomResources() {
    final List<ZipResource> customResources = new LinkedList<>();

    for (ApplicationFileBuilder application : applications) {
      final File applicationFile = application.getArtifactFile();
      customResources.add(new ZipResource(applicationFile.getAbsolutePath(), "apps/" + applicationFile.getName()));
    }

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

    customResources.add(new ZipResource(domainDescriptor.getAbsolutePath(), MULE_DOMAIN_JSON_LOCATION));
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
    MuleDomainModel.MuleDomainModelBuilder MuleDomainModelBuilder =
        new MuleDomainModel.MuleDomainModelBuilder();
    MuleDomainModelBuilder.setName(getArtifactId()).setMinMuleVersion("4.0.0");
    redeploymentEnabled.ifPresent(MuleDomainModelBuilder::setRedeploymentEnabled);
    configResources.ifPresent(configs -> {
      String[] configFiles = configs.split(",");
      MuleDomainModelBuilder.setConfigs(asList(configFiles));
    });
    MuleDomainModelBuilder.withClassLoaderModelDescriber().setId(MAVEN);
    exportedResources.ifPresent(resources -> {
      MuleDomainModelBuilder.withClassLoaderModelDescriber().addProperty(EXPORTED_RESOURCES, resources.split(","));
    });
    MuleDomainModelBuilder.withBundleDescriptorLoader(new MuleArtifactLoaderDescriptor(MAVEN, emptyMap()));
    String applicationDescriptorContent = new MuleDomainModelJsonSerializer().serialize(MuleDomainModelBuilder.build());
    try (FileWriter fileWriter = new FileWriter(domainDescriptor)) {
      fileWriter.write(applicationDescriptorContent);
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
    return domainDescriptor;
  }
}
