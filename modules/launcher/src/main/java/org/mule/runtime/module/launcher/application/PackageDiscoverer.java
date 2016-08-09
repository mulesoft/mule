/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.application;

import java.net.URL;
import java.util.Set;

/**
 * Discovers Java packages
 */
public interface PackageDiscoverer {

  /**
   * Finds the packages defined in a given resource
   *
   * @param library folder or JAR file to explore. Non null
   * @return the packages found on the resource or an empty set if the resource is not of an expected type.
   */
  Set<String> findPackages(URL library);
}
