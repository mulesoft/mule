/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.internal.artifact;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import java.util.Optional;
import java.util.Properties;

import org.mule.runtime.api.service.Service;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

/**
 * Describes how to create a {@link Service} instance.
 */
public class ServiceDescriptor extends ArtifactDescriptor {

  public static final String SERVICE_PROPERTIES = "service.properties";

  private String serviceProviderClassName;

  /**
   * Creates a new service descriptor
   *
   * @param name service name. Non empty.
   */
  public ServiceDescriptor(String name) {
    super(name);
  }

  /**
   * Creates a new service descriptor
   *
   * @param name service name. Non empty.
   */
  public ServiceDescriptor(String name, Optional<Properties> properties) {
    super(name, properties);
  }

  public String getServiceProviderClassName() {
    return serviceProviderClassName;
  }

  public void setServiceProviderClassName(String serviceProviderClassName) {
    checkArgument(!isEmpty(serviceProviderClassName), "serviceProviderClassName cannot be empty");

    this.serviceProviderClassName = serviceProviderClassName;
  }
}
