/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.api;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableSet;

import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.jpms.api.MuleContainerModule;
import org.mule.runtime.module.artifact.api.classloader.ExportedService;

import java.util.List;
import java.util.Set;

/**
 * Defines a module on the Mule container as defined by a {@code mule-module.properties} descriptor.
 */
public class MuleModule implements MuleContainerModule {

  private final String name;
  private final Set<String> exportedPackages;
  private final Set<String> exportedPaths;
  private final Set<String> privilegedExportedPackages;
  private final Set<String> privilegedArtifacts;
  private final List<ExportedService> exportedServices;

  /**
   * Creates a new module
   *
   * @param name                       module name. Not empty.
   * @param exportedPackages           java packages exported by this module. Not null.
   * @param exportedPaths              java resources exported by this module. Not null;
   * @param privilegedExportedPackages java packages exported by this module to privileged artifacts only. Not null.
   * @param privilegedArtifacts        artifacts with privileged access to the API. Each artifact is defined using the artifact's
   *                                   Maven groupId:artifactId. Non null.
   * @param exportedServices           contains the definition of service implementations that must be accessible to artifacts via
   *                                   SPI. Non null.
   */
  public MuleModule(String name, Set<String> exportedPackages, Set<String> exportedPaths, Set<String> privilegedExportedPackages,
                    Set<String> privilegedArtifacts, List<ExportedService> exportedServices) {
    checkArgument(!StringUtils.isEmpty(name), "name cannot be empty");
    checkArgument(exportedPackages != null, "exportedPackages cannot be null");
    checkArgument(exportedPaths != null, "exportedPaths cannot be null");
    checkArgument(!containsMetaInfServicesResource(exportedPaths), "exportedPaths cannot contain paths on META-INF/services");
    checkArgument(privilegedExportedPackages != null, "privilegedExportedPackages cannot be null");
    checkArgument(privilegedArtifacts != null, "privilegedArtifacts cannot be null");
    checkArgument(exportedServices != null, "exportedServices cannot be null");

    this.name = name;
    this.exportedPackages = unmodifiableSet(exportedPackages);
    this.exportedPaths = unmodifiableSet(exportedPaths);
    this.privilegedExportedPackages = privilegedExportedPackages;
    this.privilegedArtifacts = privilegedArtifacts;
    this.exportedServices = exportedServices;
  }

  private boolean containsMetaInfServicesResource(Set<String> exportedPaths) {
    return exportedPaths.stream().filter(s -> s.startsWith("META-INF/services")).findAny().isPresent();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Set<String> getExportedPackages() {
    return exportedPackages;
  }

  @Override
  public Set<String> getExportedPaths() {
    return exportedPaths;
  }

  @Override
  public Set<String> getPrivilegedExportedPackages() {
    return privilegedExportedPackages;
  }

  @Override
  public Set<String> getPrivilegedArtifacts() {
    return privilegedArtifacts;
  }

  public List<ExportedService> getExportedServices() {
    return exportedServices != null ? exportedServices : emptyList();
  }

  @Override
  public String toString() {
    return this.getClass().getName() + "[" + name + "]";
  }
}
