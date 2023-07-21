/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.classloader;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.api.annotation.NoInstantiate;

import java.net.URL;

/**
 * Defines a service that will be exported by a module to other Mule artifacts via SPI.
 *
 * @since 4.0
 */
@NoInstantiate
public final class ExportedService {

  private final String serviceInterface;
  private final URL resource;

  /**
   * Create a new service
   *
   * @param serviceInterface fully qualified name of the interface that defines the service to be located using SPI. Non empty.
   * @param resource         resource to be returned when {code serviceInterface} is searched via SPI. Non null.
   */
  public ExportedService(String serviceInterface, URL resource) {
    checkArgument(!isEmpty(serviceInterface), "serviceInterface cannot be empty");
    checkArgument(resource != null, "resource cannot be null");

    this.serviceInterface = serviceInterface;
    this.resource = resource;
  }

  public String getServiceInterface() {
    return serviceInterface;
  }

  public URL getResource() {
    return resource;
  }
}
