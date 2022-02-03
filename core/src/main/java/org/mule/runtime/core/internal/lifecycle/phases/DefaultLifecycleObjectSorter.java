/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lifecycle.phases;

import org.mule.runtime.core.internal.registry.Registry;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Default implementation of {@link LifecycleObjectSorter}.
 * <p>
 * It works by classifying objects into buckets depending their type, to finally merge all the buckets together
 *
 * @since 4.2
 */
public class DefaultLifecycleObjectSorter implements LifecycleObjectSorter {

  private List<Object>[] buckets;
  private int objectCount = 0;
  protected Class<?>[] orderedLifecycleTypes;

  /**
   * Creates a new instance
   *
   * @param orderedLifecycleTypes an ordered array specifying a type based order
   */
  public DefaultLifecycleObjectSorter(Class<?>[] orderedLifecycleTypes) {
    this.orderedLifecycleTypes = orderedLifecycleTypes;
    buckets = new List[orderedLifecycleTypes.length];
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addObject(String name, Object object) {
    for (int i = 0; i < orderedLifecycleTypes.length; i++) {
      if (orderedLifecycleTypes[i].isInstance(object)) {
        List<Object> bucket = buckets[i];
        if (bucket == null) {
          bucket = new LinkedList<>();
          buckets[i] = bucket;
        }
        objectCount += doAddObject(name, object, bucket);
        break;
      }
    }
  }

  /**
   * Actually adds the given {@code object} to the given {@code bucket}.
   * <p>
   * Implementors are free to add additional objects to the bucket, in any particular position. This default implementation
   * however only adds the given one at the end of the list
   *
   * @param name   the name under which the object is registered in the {@link Registry}
   * @param object the object
   * @param bucket the bucket in which the object(s) are to be added
   * @return how many objects were added
   */
  protected int doAddObject(String name, Object object, List<Object> bucket) {
    bucket.add(object);
    return 1;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Object> getSortedObjects() {
    List<Object> sorted = new ArrayList<>(objectCount);
    for (List<Object> bucket : buckets) {
      if (bucket != null) {
        sorted.addAll(bucket);
      }
    }

    return sorted;
  }

}
