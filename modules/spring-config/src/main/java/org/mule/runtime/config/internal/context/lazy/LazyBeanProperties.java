/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context.lazy;

/**
 * Bean constants names used for lazy-initialization.
 */
public final class LazyBeanProperties {

  public static final String SHARED_PARTITIONED_PERSISTENT_OBJECT_STORE_PATH = "_sharedPartitionatedPersistentObjectStorePath";

  public static final String LAZY_MULE_OBJECT_STORE_MANAGER = "_muleLazyObjectStoreManager";
  public static final String LAZY_MULE_RUNTIME_LOCK_FACTORY = "_muleLazyRuntimeLockFactory";

  private LazyBeanProperties() {
    // nothing to do
  }
}
