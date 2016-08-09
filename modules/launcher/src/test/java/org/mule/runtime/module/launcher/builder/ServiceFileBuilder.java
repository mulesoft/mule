/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static org.mule.runtime.module.launcher.service.ServiceDescriptor.SERVICE_PROPERTIES;
import org.mule.runtime.core.util.StringUtils;
import org.mule.tck.ZipUtils;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Creates Service files.
 */
public class ServiceFileBuilder extends AbstractArtifactFileBuilder<ServiceFileBuilder> {

  private Properties properties = new Properties();

  /**
   * Creates a new builder
   *
   * @param id artifact identifier. Non empty.
   */
  public ServiceFileBuilder(String id) {
    super(id);
  }

  /**
   * Creates a new builder from another instance.
   *
   * @param source instance used as template to build the new one. Non null.
   */
  public ServiceFileBuilder(ServiceFileBuilder source) {
    super(source);
  }

  /**
   * Create a new builder from another instance and different ID.
   *
   * @param id artifact identifier. Non empty.
   * @param source instance used as template to build the new one. Non null.
   */
  public ServiceFileBuilder(String id, ServiceFileBuilder source) {
    super(id, source);
    this.properties.putAll(source.properties);
  }

  @Override
  protected ServiceFileBuilder getThis() {
    return this;
  }

  /**
   * Adds a property into the service properties file.
   *
   * @param propertyName name fo the property to add. Non empty
   * @param propertyValue value of the property to add. Non null.
   * @return the same builder instance
   */
  public ServiceFileBuilder configuredWith(String propertyName, String propertyValue) {
    checkImmutable();
    checkArgument(!StringUtils.isEmpty(propertyName), "Property name cannot be empty");
    checkArgument(propertyValue != null, "Property value cannot be null");
    properties.put(propertyName, propertyValue);
    return this;
  }

  @Override
  protected List<ZipUtils.ZipResource> getCustomResources() throws Exception {
    final List<ZipUtils.ZipResource> customResources = new LinkedList<>();

    if (!properties.isEmpty()) {
      final File applicationPropertiesFile = new File(getTempFolder(), SERVICE_PROPERTIES);
      applicationPropertiesFile.deleteOnExit();
      createPropertiesFile(applicationPropertiesFile, properties);

      customResources.add(new ZipUtils.ZipResource(applicationPropertiesFile.getAbsolutePath(), SERVICE_PROPERTIES));
    }

    return customResources;
  }

  @Override
  public String getConfigFile() {
    return null;
  }
}
