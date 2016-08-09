/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.service;

import static org.mule.runtime.module.launcher.service.ServiceDescriptor.SERVICE_PROPERTIES;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * Creates {@link ServiceDescriptor} instances.
 */
public class ServiceDescriptorFactory implements ArtifactDescriptorFactory<ServiceDescriptor> {

  public static final String SERVICE_PROVIDER_CLASS_NAME = "service.className";

  @Override
  public ServiceDescriptor create(File artifactFolder) throws ArtifactDescriptorCreateException {
    if (!artifactFolder.exists()) {
      throw new IllegalArgumentException("Service folder does not exists: " + artifactFolder.getAbsolutePath());
    }
    final String serviceName = artifactFolder.getName();
    final ServiceDescriptor descriptor = new ServiceDescriptor();
    descriptor.setRootFolder(artifactFolder);
    descriptor.setName(serviceName);

    final File servicePropsFile = new File(artifactFolder, SERVICE_PROPERTIES);
    if (!servicePropsFile.exists()) {

      throw new ArtifactDescriptorCreateException("Service must contain a " + SERVICE_PROPERTIES + " file");
    }

    Properties props = new Properties();
    try {
      props.load(new FileReader(servicePropsFile));
    } catch (IOException e) {
      throw new ArtifactDescriptorCreateException("Cannot read service.properties file", e);
    }

    descriptor.setServiceProviderClassName(props.getProperty(SERVICE_PROVIDER_CLASS_NAME));

    return descriptor;
  }
}
