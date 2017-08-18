/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.classloader;

/**
 * Enables access {@link ClassLoaderLookupPolicy} used on a given classLoader.
 */
public interface ClassLoaderLookupPolicyProvider {

  /**
   * @return lookup policy used on the classLoader. Non null.
   */
  ClassLoaderLookupPolicy getClassLoaderLookupPolicy();
}
