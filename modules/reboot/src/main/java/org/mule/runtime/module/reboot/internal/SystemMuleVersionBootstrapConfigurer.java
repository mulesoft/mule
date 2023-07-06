/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.reboot.internal;

import static java.lang.String.format;
import static java.lang.System.setProperty;

import org.mule.runtime.module.reboot.api.MuleContainerBootstrapUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.Properties;

/**
 * A {@link BootstrapConfigurer} which takes care of setting the mule.version system properties based on the given POM file.
 */
public class SystemMuleVersionBootstrapConfigurer implements BootstrapConfigurer {

  private final String pomFilePath;

  public SystemMuleVersionBootstrapConfigurer(String pomFilePath) {
    this.pomFilePath = pomFilePath;
  }

  @Override
  public void configure() throws BootstrapConfigurationException {
    try (InputStream propertiesStream = getPropertiesStream()) {
      Properties mavenProperties = new Properties();
      mavenProperties.load(propertiesStream);

      String version = mavenProperties.getProperty("version");
      setProperty("mule.version", version);
      setProperty("mule.reference.version", version + '-' + (new Date()).getTime());
    } catch (Exception e) {
      String errorMessage = format("Error retrieving property 'version' from pom.properties file: %s", pomFilePath);
      throw new BootstrapConfigurationException(3, errorMessage, e);
    }
  }

  private InputStream getPropertiesStream() throws IOException {
    URL mavenPropertiesUrl = MuleContainerBootstrapUtils.getResource(pomFilePath, this.getClass());
    return mavenPropertiesUrl.openStream();
  }
}
