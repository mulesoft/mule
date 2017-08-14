/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
