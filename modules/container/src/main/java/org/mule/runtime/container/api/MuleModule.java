/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.api;

import static java.util.Collections.unmodifiableSet;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.core.util.StringUtils;

import java.util.Set;

/**
 * Defines a module on the Mule container
 */
public class MuleModule {

  protected static final String INVALID_PRIVILEGED_API_DEFINITION_ERROR =
      "Invalid privileged API: both privileged packages and artifacts must be defined";

  private final String name;
  private final Set<String> exportedPackages;
  private final Set<String> exportedPaths;
  private final Set<String> privilegedExportedPackages;
  private final Set<String> privilegedArtifacts;

  /**
   * Creates a new module
   *
   * @param name module name. Not empty.
   * @param exportedPackages java packages exported by this module. Not null.
   * @param exportedPaths java resources exported by this module. Not null;
   * @param privilegedExportedPackages java packages exported by this module to privileged artifacts only. Not null.
   * @param privilegedArtifacts name of the artifacts with privileged access to the API. Non null.
   */
  public MuleModule(String name, Set<String> exportedPackages, Set<String> exportedPaths, Set<String> privilegedExportedPackages,
                    Set<String> privilegedArtifacts) {
    checkArgument(!StringUtils.isEmpty(name), "name cannot be empty");
    checkArgument(exportedPackages != null, "exportedPackages cannot be null");
    checkArgument(exportedPaths != null, "exportedPaths cannot be null");
    checkArgument(privilegedExportedPackages != null, "privilegedExportedPackages cannot be null");
    checkArgument(privilegedArtifacts != null, "privilegedArtifacts cannot be null");
    checkArgument((privilegedArtifacts.isEmpty() && privilegedExportedPackages.isEmpty())
        || (!privilegedArtifacts.isEmpty() && !privilegedExportedPackages.isEmpty()), INVALID_PRIVILEGED_API_DEFINITION_ERROR);
    this.name = name;
    this.exportedPackages = unmodifiableSet(exportedPackages);
    this.exportedPaths = unmodifiableSet(exportedPaths);
    this.privilegedExportedPackages = privilegedExportedPackages;
    this.privilegedArtifacts = privilegedArtifacts;
  }

  public String getName() {
    return name;
  }

  public Set<String> getExportedPackages() {
    return exportedPackages;
  }

  public Set<String> getExportedPaths() {
    return exportedPaths;
  }

  public Set<String> getPrivilegedExportedPackages() {
    return privilegedExportedPackages;
  }

  public Set<String> getPrivilegedArtifacts() {
    return privilegedArtifacts;
  }
}
