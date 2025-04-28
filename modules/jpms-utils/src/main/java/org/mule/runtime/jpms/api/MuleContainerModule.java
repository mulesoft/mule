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

  /**
   * @return the name of the module, as defined in the module itself.
   */
  public String getName();

  /**
   * @return the Java packages exported by this module.
   */
  public Set<String> getExportedPackages();

  /**
   * @return the Java resources exported by this module.
   */
  public Set<String> getExportedPaths();

  /**
   * @return the Java packages exported by this module to {@link #getPrivilegedArtifacts() privileged artifacts} only.
   */
  public Set<String> getPrivilegedExportedPackages();

  /**
   * @return the artifacts with privileged access to the API. Each artifact is defined using the artifact's Maven
   *         {@code groupId:artifactId}.
   */
  public Set<String> getPrivilegedArtifacts();

}
