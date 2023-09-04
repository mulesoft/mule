/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.jpms.api;

import java.util.Set;

/**
 * Defines a module on the Mule container
 * 
 * @since 4.6 (extracted from org.mule.runtime.container.api.MuleModule)
 */
public interface MuleContainerModule {

  public String getName();

  public Set<String> getExportedPackages();

  public Set<String> getExportedPaths();

  public Set<String> getPrivilegedExportedPackages();

  public Set<String> getPrivilegedArtifacts();

  // public List<ExportedService> getExportedServices();

}
