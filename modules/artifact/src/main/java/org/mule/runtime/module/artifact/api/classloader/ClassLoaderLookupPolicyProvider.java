/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.classloader;

import org.mule.api.annotation.NoImplement;

/**
 * Enables access {@link ClassLoaderLookupPolicy} used on a given classLoader.
 */
@NoImplement
public interface ClassLoaderLookupPolicyProvider {

  /**
   * @return lookup policy used on the classLoader. Non null.
   */
  ClassLoaderLookupPolicy getClassLoaderLookupPolicy();
}
