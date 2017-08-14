/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.classloader.exception;

import static java.lang.String.format;

import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;

/**
 * Extends {@link ClassNotFoundException} providing additional troubleshooting information from the context of the
 * {@link RegionClassLoader}.
 */
public class ClassNotFoundInRegionException extends ClassNotFoundException {

  private static final long serialVersionUID = -2800293812538208279L;

  private String className;
  private String regionName;
  private String artifactName;

  /**
   * Builds the exception.
   * 
   * @param className the name of the class that was trying to be loaded.
   * @param regionName the name of the region the class was being loaded from.
   */
  public ClassNotFoundInRegionException(String className, String regionName) {
    super(format("Class '%s' has no package mapping for region '%s'.", className, regionName));
    this.className = className;
    this.regionName = regionName;
  }

  /**
   * Builds the exception.
   * 
   * @param className the name of the class that was trying to be loaded.
   * @param regionName the name of the region the class was being loaded from.
   * @param artifactName the name of the artifact in the region the class was being loaded from.
   * @param cause the actual exception that was thrown when loading the class form the artifact classLoader.
   */
  public ClassNotFoundInRegionException(String className, String regionName, String artifactName, ClassNotFoundException cause) {
    super(format("Class '%s' not found in classloader for artifact '%s' in region '%s'.", className, artifactName, regionName),
          cause);
    this.className = className;
    this.regionName = regionName;
    this.artifactName = artifactName;
  }

  /**
   * @return the name of the class that was trying to be loaded.
   */
  public String getClassName() {
    return className;
  }

  /**
   * @return the name of the region the class was being loaded from.
   */
  public String getRegionName() {
    return regionName;
  }

  /**
   * @return the name of the artifact in the region the class was being loaded from.
   */
  public String getArtifactName() {
    return artifactName;
  }
}
