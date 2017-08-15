/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader.exception;

import static java.lang.Boolean.valueOf;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.lang.System.lineSeparator;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_LOG_VERBOSE_CLASSLOADING;

import org.mule.runtime.module.artifact.api.classloader.ClassLoaderFilter;
import org.mule.runtime.module.artifact.api.classloader.FilteringArtifactClassLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extends {@link ClassNotFoundException} providing additional troubleshooting information from the context of the
 * {@link FilteringArtifactClassLoader}.
 */
public class NotExportedClassException extends ClassNotFoundException {

  private static final Logger logger = LoggerFactory.getLogger(NotExportedClassException.class);

  private static final long serialVersionUID = 2510347069070514569L;

  private String className;
  private String artifactName;
  private ClassLoaderFilter filter;

  /**
   * Builds the exception.
   * 
   * @param className the name of the class that was trying to be loaded.
   * @param artifactName the name of the artifact the class was being loaded from.
   * @param filter the applied filter for the artifact.
   */
  public NotExportedClassException(String className, String artifactName, ClassLoaderFilter filter) {
    super(format("Class '%s' not found in classloader for artifact '%s'.", className, artifactName));
    this.className = className;
    this.artifactName = artifactName;
    this.filter = filter;
  }

  /**
   * @return the name of the class that was trying to be loaded.
   */
  public String getClassName() {
    return className;
  }

  /**
   * @return the name of the artifact the class was being loaded from.
   */
  public String getArtifactName() {
    return artifactName;
  }

  /**
   * @return the applied filter for the artifact.
   */
  public ClassLoaderFilter getFilter() {
    return filter;
  }

  @Override
  public String getMessage() {
    if (valueOf(getProperty(MULE_LOG_VERBOSE_CLASSLOADING)) || logger.isTraceEnabled()) {
      return super.getMessage() + lineSeparator() + filter.toString();
    } else {
      return super.getMessage();
    }
  }
}
