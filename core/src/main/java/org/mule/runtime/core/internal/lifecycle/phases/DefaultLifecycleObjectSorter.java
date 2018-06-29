/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lifecycle.phases;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DefaultLifecycleObjectSorter implements LifecycleObjectSorter {

  private List<Object>[] buckets;
  private int objectCount = 0;
  protected Class<?>[] orderedLifecycleTypes;

  public DefaultLifecycleObjectSorter(Class<?>[] orderedLifecycleTypes) {
    this.orderedLifecycleTypes = orderedLifecycleTypes;
    buckets = new List[orderedLifecycleTypes.length];
  }

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

  protected int doAddObject(String name, Object object, List<Object> bucket) {
    bucket.add(object);
    return 1;
  }

  @Override
  public List<Object> getSortedList() {
    List<Object> sorted = new ArrayList<>(objectCount);
    for (List<Object> bucket : buckets) {
      if (bucket != null) {
        sorted.addAll(bucket);
      }
    }

    return sorted;
  }
}
