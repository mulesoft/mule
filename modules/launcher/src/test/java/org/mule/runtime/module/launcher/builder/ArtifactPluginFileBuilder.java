/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.builder;

import static com.google.common.base.Preconditions.checkArgument;
import org.mule.tck.ZipUtils.ZipResource;
import static org.mule.runtime.module.launcher.plugin.ArtifactPluginDescriptor.PLUGIN_PROPERTIES;
import org.mule.runtime.core.util.StringUtils;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Creates Mule Application Plugin files.
 */
public class ArtifactPluginFileBuilder extends AbstractArtifactFileBuilder<ArtifactPluginFileBuilder> {

  private Properties properties = new Properties();

  /**
   * Creates a new builder
   *
   * @param id artifact identifier. Non empty.
   */
  public ArtifactPluginFileBuilder(String id) {
    super(id);
  }

  /**
   * Creates a new builder from another instance.
   *
   * @param source instance used as template to build the new one. Non null.
   */
  public ArtifactPluginFileBuilder(ArtifactPluginFileBuilder source) {
    super(source);
  }

  /**
   * Create a new builder from another instance and different ID.
   *
   * @param id artifact identifier. Non empty.
   * @param source instance used as template to build the new one. Non null.
   */
  public ArtifactPluginFileBuilder(String id, ArtifactPluginFileBuilder source) {
    super(id, source);
    this.properties.putAll(source.properties);
  }

  @Override
  protected ArtifactPluginFileBuilder getThis() {
    return this;
  }

  /**
   * Adds a property into the plugin properties file.
   *
   * @param propertyName name fo the property to add. Non empty
   * @param propertyValue value of the property to add. Non null.
   * @return the same builder instance
   */
  public ArtifactPluginFileBuilder configuredWith(String propertyName, String propertyValue) {
    checkImmutable();
    checkArgument(!StringUtils.isEmpty(propertyName), "Property name cannot be empty");
    checkArgument(propertyValue != null, "Property value cannot be null");
    properties.put(propertyName, propertyValue);
    return this;
  }

  /**
   * Adds a class file to the application classes folder.
   *
   * @param classFile class file from a external file or test resource.
   * @return the same builder instance
   */
  public ArtifactPluginFileBuilder containingClass(String classFile) {
    checkImmutable();
    checkArgument(!StringUtils.isEmpty(classFile), "Class file cannot be empty");
    String alias = classFile.replace(".clazz", ".class");
    resources.add(new ZipResource(classFile, "classes/" + alias));
    return this;
  }

  /**
   * Adds a resource file to the application classes folder.
   *
   * @param resourceFile resource file from a external file or test resource.
   * @return the same builder instance
   */
  public ArtifactPluginFileBuilder containingResource(String resourceFile, String alias) {
    checkImmutable();
    checkArgument(!StringUtils.isEmpty(resourceFile), "Resource file cannot be empty");
    resources.add(new ZipResource(resourceFile, "classes/" + alias));
    return this;
  }

  @Override
  protected List<ZipResource> getCustomResources() throws Exception {
    final List<ZipResource> customResources = new LinkedList<>();

    if (!properties.isEmpty()) {
      final File applicationPropertiesFile = new File(getTempFolder(), PLUGIN_PROPERTIES);
      applicationPropertiesFile.deleteOnExit();
      createPropertiesFile(applicationPropertiesFile, properties);

      customResources.add(new ZipResource(applicationPropertiesFile.getAbsolutePath(), PLUGIN_PROPERTIES));
    }

    return customResources;
  }

  @Override
  public String getConfigFile() {
    return null;
  }
}
