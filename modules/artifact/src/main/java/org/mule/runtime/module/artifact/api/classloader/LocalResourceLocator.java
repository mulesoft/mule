/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.classloader;

import org.mule.api.annotation.NoImplement;

import java.net.URL;

@NoImplement
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
