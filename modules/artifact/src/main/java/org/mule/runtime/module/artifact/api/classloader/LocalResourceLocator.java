/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader;

import java.net.URL;

public interface LocalResourceLocator {

  /**
   * This method finds a resource in the local environment for this artifact, if the resource is not found and the artifact has a
   * parent artifact then the operation is resolved by that parent, if there is no parent, the resources is searched in the global
   * environment.
   * 
   * @param resourceName name of the resource to find.
   * @return the resource URL, null if it doesn't exists.
   */
  URL findLocalResource(String resourceName);
}
