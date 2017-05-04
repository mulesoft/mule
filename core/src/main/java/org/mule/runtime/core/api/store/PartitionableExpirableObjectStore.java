/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.store;

import java.io.Serializable;

public interface PartitionableExpirableObjectStore<T extends Serializable>
    extends ExpirableObjectStore<T>, PartitionableObjectStore<T> {

  void expire(long entryTTL, int maxEntries, String partitionName) throws ObjectStoreException;
}
