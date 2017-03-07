/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.embedded.api;

import java.io.Serializable;
import java.net.URL;
import java.util.List;

/**
 * This class contains all the information related to the container resources.
 */
public class ContainerInfo implements Serializable {

  private final String version;
  private final URL containerBaseFolder;
  private final List<URL> services;

  /**
   * Creates a new instance.
   *
   * @param version the version of the mule runtime to use.
   * @param containerBaseFolder the container base folder to use for placing container resources.
   * @param services the list of services to load within the container.
   */
  public ContainerInfo(String version, URL containerBaseFolder, List<URL> services) {
    this.version = version;
    this.containerBaseFolder = containerBaseFolder;
    this.services = services;
  }

  /**
   * @return the runtime version to use
   */
  public String getVersion() {
    return version;
  }

  /**
   * @return the folder to use to create container resources
   */
  public URL getContainerBaseFolder() {
    return containerBaseFolder;
  }

  /**
   * @return the list of services to load in the container
   */
  public List<URL> getServices() {
    return services;
  }
}
