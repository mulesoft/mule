/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.internal.util;

import java.net.URI;

/**
 * Explores jar files or exploded jar folders to find packages and resources.
 */
public interface JarExplorer {

  /**
   * Finds the packages defined in a given resource
   *
   * @param library folder or JAR file to explore. Non null
   * @return the {@link JarInfo} containing the found resources and packages. Non null.
   */
  JarInfo explore(URI library);
}
