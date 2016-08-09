/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.classloader;

/**
 * Indicates how to resolve class loading lookup related to parent/child relationships between two different classLoaders.
 */
public enum ClassLoaderLookupStrategy {

  /**
   * Class will be looked up from the parent classloader first. If the class is found, then it will be returned. Otherwise lookup
   * process will continue on the child classloader. If the class is found, then it will be returned, otherwise a
   * {@link ClassNotFoundException} will be thrown.
   */
  PARENT_FIRST,

  /**
   * Class will be looked up from the parent classloader only. If the class is found, then it will be returned, otherwise a
   * {@link ClassNotFoundException} will be thrown.
   */
  PARENT_ONLY,

  /**
   * Class will be looked up from the child classloader first. If the class is found, then it will be returned. Otherwise lookup
   * process will continue on the parent classloader. If the class is found, then it will be returned, otherwise a
   * {@link ClassNotFoundException} will be thrown.
   */
  CHILD_FIRST
}
