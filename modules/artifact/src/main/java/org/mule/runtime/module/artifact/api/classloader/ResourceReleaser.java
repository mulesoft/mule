/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader;

/**
 * Implementations of this class should take care about resources that may leak memory after application undeployment. Mule
 * ensures to create an instance of this class with the same class loader that loaded the application resources in order to ensure
 * the access to them.
 */
public interface ResourceReleaser {

  /**
   * Attempts to release, during an {@link Artifact} undeployment, resources that were not explicitly released and could cause a
   * memory leak.
   */
  void release();
}
