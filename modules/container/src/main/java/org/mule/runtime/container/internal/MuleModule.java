/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.internal;

import static java.util.Collections.unmodifiableSet;
import static org.mule.runtime.core.util.Preconditions.checkArgument;

import org.mule.runtime.core.util.StringUtils;

import java.util.Set;

/**
 * Provides module definition information
 */
public class MuleModule {

  private final String name;
  private final Set<String> exportedPackages;
  private final Set<String> exportedPaths;


  /**
   * Creates a new module
   *
   * @param name module name. Not empty.
   * @param exportedPackages java packages exported by this module. Not null.
   * @param exportedPaths java resources exported by this module. Not null;
   */
  public MuleModule(String name, Set<String> exportedPackages, Set<String> exportedPaths) {
    checkArgument(!StringUtils.isEmpty(name), "Name cannot be empty");
    checkArgument(exportedPackages != null, "ExportedPackages cannot be null");
    checkArgument(exportedPaths != null, "ExportedPaths cannot be null");
    this.name = name;
    this.exportedPackages = unmodifiableSet(exportedPackages);
    this.exportedPaths = unmodifiableSet(exportedPaths);
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
}
