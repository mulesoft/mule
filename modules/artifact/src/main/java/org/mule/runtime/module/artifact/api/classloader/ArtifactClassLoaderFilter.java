/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.classloader;

import java.util.Set;

/**
 * Filter that provides access to the configured exported packages and resources.
 */
public interface ArtifactClassLoaderFilter extends ClassLoaderFilter {


  /**
   * @return filter's exported class packages. Non null
   */
  Set<String> getExportedClassPackages();

  /**
   * @return filter's exported resources. Non null
   */
  Set<String> getExportedResources();
}
