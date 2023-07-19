/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.internal.util;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.module.artifact.api.classloader.ExportedService;

import java.util.List;
import java.util.Set;

/**
 * Provides information about packages and resources found in a Java JAR.
 */
public class JarInfo {

  private final Set<String> packages;
  private final Set<String> resources;
  private final List<ExportedService> services;

  /**
   * Creates a new instance corresponding to a given JAR.
   *
   * @param packages  Java packages found on the JAR. Non null.
   * @param resources Java resources found on the JAR. Non null.
   * @param services  SPI services definitions found on the JAR. Non null.
   */
  public JarInfo(Set<String> packages, Set<String> resources, List<ExportedService> services) {
    checkArgument(packages != null, "Packages cannot be null");
    checkArgument(resources != null, "Resources cannot be null");
    checkArgument(services != null, "Services cannot be null");
    this.packages = packages;
    this.resources = resources;
    this.services = services;
  }

  /**
   * @return Java packages found on the JAR. Non null.
   */
  public Set<String> getPackages() {
    return packages;
  }

  /**
   * @return Java resources found on the JAR. Non null.
   */
  public Set<String> getResources() {
    return resources;
  }

  /**
   * @return SPI services definitions found on the JAR. Non null.
   */
  public List<ExportedService> getServices() {
    return services;
  }
}
