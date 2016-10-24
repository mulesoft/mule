/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor.DEFAULT_DEPLOY_PROPERTIES_RESOURCE;
import static org.mule.runtime.deployment.model.api.application.ApplicationDescriptor.DEFAULT_APP_PROPERTIES_RESOURCE;
import static org.mule.runtime.deployment.model.api.application.ApplicationDescriptor.DEFAULT_CONFIGURATION_RESOURCE;
import org.mule.runtime.core.util.FilenameUtils;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.module.artifact.builder.AbstractArtifactFileBuilder;
import org.mule.tck.ZipUtils.ZipResource;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Creates Mule Application files.
 */
public class ApplicationFileBuilder extends AbstractArtifactFileBuilder<ApplicationFileBuilder> {

  private List<ArtifactPluginFileBuilder> plugins = new LinkedList<>();
  private Properties properties = new Properties();
  private Properties deployProperties = new Properties();

  /**
   * Creates a new builder
   *
   * @param id artifact identifier. Non empty.
   */
  public ApplicationFileBuilder(String id) {
    super(id);
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
    this.plugins.addAll(source.plugins);
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
   * Adds a property into the application deployment properties file.
   *
   * @param propertyName name fo the property to add. Non empty
   * @param propertyValue value of the property to add. Non null.
   * @return the same builder instance
   */
  public ApplicationFileBuilder deployedWith(String propertyName, String propertyValue) {
    checkImmutable();
    checkArgument(!StringUtils.isEmpty(propertyName), "Property name cannot be empty");
    checkArgument(propertyValue != null, "Property value cannot be null");
    deployProperties.put(propertyName, propertyValue);
    return this;
  }

  /**
   * Adds a resource file to the artifact classes folder.
   *
   * @param resourceFile class file from a external file or test resource. Non empty.
   * @param targetFile name to use on the added resource. Non empty.
   * @return the same builder instance
   */
  public ApplicationFileBuilder usingResource(String resourceFile, String targetFile) {
    checkImmutable();
    checkArgument(!isEmpty(resourceFile), "Resource file cannot be empty");
    resources.add(new ZipResource(resourceFile, "classes/" + targetFile));

    return getThis();
  }

  /**
   * Adds an application plugin to the application.
   *
   * @param plugin builder defining the plugin. Non null.
   * @return the same builder instance
   */
  public ApplicationFileBuilder containingPlugin(ArtifactPluginFileBuilder plugin) {
    checkImmutable();
    checkArgument(plugin != null, "Plugin cannot be null");
    this.plugins.add(plugin);

    return this;
  }

  /**
   * Adds a jar file to the application plugin lib folder.
   *
   * @param jarFile jar file from a external file or test resource.
   * @return the same builder instance
   */
  public ApplicationFileBuilder sharingLibrary(String jarFile) {
    checkImmutable();
    checkArgument(!StringUtils.isEmpty(jarFile), "Jar file cannot be empty");
    resources.add(new ZipResource(jarFile, "plugins/lib/" + FilenameUtils.getName(jarFile)));

    return this;
  }

  @Override
  public String getConfigFile() {
    return DEFAULT_CONFIGURATION_RESOURCE;
  }

  @Override
  protected List<ZipResource> getCustomResources() {
    final List<ZipResource> customResources = new LinkedList<>();

    for (ArtifactPluginFileBuilder plugin : plugins) {
      customResources
          .add(new ZipResource(plugin.getArtifactFile().getAbsolutePath(), "plugins/" + plugin.getArtifactFile().getName()));
    }

    final ZipResource appProperties =
        createPropertiesFile(this.properties, DEFAULT_APP_PROPERTIES_RESOURCE);
    if (appProperties != null) {
      customResources.add(appProperties);
    }

    final ZipResource deployProperties = createPropertiesFile(this.deployProperties, DEFAULT_DEPLOY_PROPERTIES_RESOURCE);
    if (deployProperties != null) {
      customResources.add(deployProperties);
    }

    return customResources;
  }
}
