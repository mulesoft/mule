/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.builder;

import static org.mule.runtime.deployment.model.api.domain.Domain.DOMAIN_CONFIG_FILE_LOCATION;
import static org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor.DEFAULT_DEPLOY_PROPERTIES_RESOURCE;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.module.artifact.builder.AbstractArtifactFileBuilder;
import org.mule.tck.ZipUtils.ZipResource;
import org.mule.runtime.core.util.StringUtils;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Creates Mule Domain files.
 */
public class DomainFileBuilder extends AbstractArtifactFileBuilder<DomainFileBuilder> {

  private List<ApplicationFileBuilder> applications = new LinkedList<>();
  private Properties deployProperties = new Properties();

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
    this.resources.add(new ZipResource(configFile, DOMAIN_CONFIG_FILE_LOCATION));

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

  /**
   * Adds a property into the domain deployment properties file.
   *
   * @param propertyName name fo the property to add. Non empty
   * @param propertyValue value of the property to add. Non null.
   * @return the same builder instance
   */
  public DomainFileBuilder deployedWith(String propertyName, String propertyValue) {
    checkImmutable();
    checkArgument(!StringUtils.isEmpty(propertyName), "Property name cannot be empty");
    checkArgument(propertyValue != null, "Property value cannot be null");
    deployProperties.put(propertyName, propertyValue);
    return this;
  }

  @Override
  protected List<ZipResource> getCustomResources() {
    final List<ZipResource> customResources = new LinkedList<>();

    for (ApplicationFileBuilder application : applications) {
      final File applicationFile = application.getArtifactFile();
      customResources.add(new ZipResource(applicationFile.getAbsolutePath(), "apps/" + applicationFile.getName()));
    }

    final ZipResource deployProperties = createPropertiesFile(this.deployProperties, DEFAULT_DEPLOY_PROPERTIES_RESOURCE);
    if (deployProperties != null) {
      customResources.add(deployProperties);
    }

    return customResources;
  }

  @Override
  public String getConfigFile() {
    return DOMAIN_CONFIG_FILE_LOCATION;
  }
}
