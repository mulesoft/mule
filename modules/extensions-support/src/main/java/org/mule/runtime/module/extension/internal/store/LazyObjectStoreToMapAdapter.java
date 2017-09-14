/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.store;

import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreToMapAdapter;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * An {@link ObjectStoreToMapAdapter} which delays effectively obtaining the {@link ObjectStore}
 * to be bridged until it's absolutely necessary.
 * <p>
 * This is useful for cases in which the adapter is to be created at a time in which the
 * object store may not be already available.
 *
 * @param <T> the generic type of the instances contained in the {@link ObjectStore}
 * @since 4.0
 */
public class LazyObjectStoreToMapAdapter<T extends Serializable> extends ObjectStoreToMapAdapter<T> {

  private final LazyValue<ObjectStore<T>> objectStore;

  public LazyObjectStoreToMapAdapter(Supplier<ObjectStore<T>> objectStoreSupplier) {
    objectStore = new LazyValue<>(objectStoreSupplier);
  }

  @Override
  public ObjectStore<T> getObjectStore() {
    return objectStore.get();
  }
}
