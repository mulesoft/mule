/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader.exception;

import static org.mule.runtime.api.exception.MuleException.MULE_VERBOSE_EXCEPTIONS;

import static java.lang.Boolean.getBoolean;

import org.mule.api.annotation.NoInstantiate;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;

/**
 * Extends {@link ClassNotFoundException} providing additional troubleshooting information from the context of the
 * {@link RegionClassLoader}.
 */
@NoInstantiate
public final class ClassNotFoundInRegionException extends ClassNotFoundException {

  private static final long serialVersionUID = -2800293812538208276L;

  private final String className;
  private final String regionName;
  private String artifactName;

  /**
   * Builds the exception.
   *
   * @param className  the name of the class that was trying to be loaded.
   * @param regionName the name of the region the class was being loaded from.
   */
  public ClassNotFoundInRegionException(String className, String regionName) {
    super("Class '" + className + "' has no package mapping for region '" + regionName + "'.");
    this.className = className;
    this.regionName = regionName;
  }

  /**
   * Builds the exception.
   *
   * @param className    the name of the class that was trying to be loaded.
   * @param regionName   the name of the region the class was being loaded from.
   * @param artifactName the name of the artifact in the region the class was being loaded from.
   * @param cause        the actual exception that was thrown when loading the class form the artifact classLoader.
   */
  public ClassNotFoundInRegionException(String className, String regionName, String artifactName, ClassNotFoundException cause) {
    super("Class '" + className + "' not found in classloader for artifact '" + artifactName + "' in region '" + regionName
        + "'.",
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

  @Override
  public synchronized Throwable fillInStackTrace() {
    // This might happen during logger initialization,
    // so the implementation from MuleException cannot be used since it requires a logger.
    if (getBoolean(MULE_VERBOSE_EXCEPTIONS)) {
      return super.fillInStackTrace();
    } else {
      return this;
    }
  }
}
