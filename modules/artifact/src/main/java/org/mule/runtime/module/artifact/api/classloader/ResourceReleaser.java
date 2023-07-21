/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.classloader;

import org.mule.api.annotation.NoImplement;

/**
 * Implementations of this class should take care about resources that may leak memory after application undeployment. Mule
 * ensures to create an instance of this class with the same class loader that loaded the application resources in order to ensure
 * the access to them.
 */
@NoImplement
@FunctionalInterface
public interface ResourceReleaser {

  /**
   * Attempts to release, during an {@link org.mule.runtime.module.artifact.api.Artifact} undeployment, resources that were not
   * explicitly released and could cause a memory leak.
   */
  void release();
}
