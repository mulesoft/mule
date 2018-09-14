/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.store;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import java.io.File;
import java.io.Serializable;

/**
 * Extends {@link PartitionedPersistentObjectStore} in order to allow using a shared path where OS data will be persisted.
 * This means also that if this is used by different MuleContext they will share the OS data.
 *
 * @param <T> the serializable entity to be persisted by OS
 *
 * @since 4.2.0, 4.1.4
 */
public class SharedPartitionedPersistentObjectStore<T extends Serializable> extends PartitionedPersistentObjectStore<T> {

  private File workingDirectory;

  /**
   * Creates a shared partitioned persistent object store.
   *
   * @param workingDirectory {@link File} where to store this OS data. Not null.
   */
  public SharedPartitionedPersistentObjectStore(File workingDirectory) {
    checkArgument(workingDirectory != null, "workingDirectory cannot be null");
    this.workingDirectory = workingDirectory;
  }

  @Override
  protected String getWorkingDirectory() {
    return workingDirectory.getAbsolutePath();
  }

}
