/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config.preferred;

import java.util.Comparator;
import java.util.Iterator;

/**
 * Selects a preferred object from a collection of instances of the same type based on the weight of the {@link Preferred}
 * annotation. If there is no preferred instances, then it just returns the first object in the collection.
 */
public class PreferredObjectSelector<T> {

  private final Comparator<T> comparator;

  public PreferredObjectSelector() {
    comparator = new Comparator<T>() {

      private PreferredComparator preferredComparator = new PreferredComparator();

      public int compare(T candidate1, T candidate2) {
        final Preferred preferred = candidate1.getClass().getAnnotation(Preferred.class);
        final Preferred preferred1 = candidate2.getClass().getAnnotation(Preferred.class);

        return preferredComparator.compare(preferred, preferred1);
      }
    };
  }

  /**
   * Selects a preferred object from instances returned by an {@link Iterator}.
   * <p/>
   * The preferred instance will be the instance annotated with {@link Preferred} annotation with the highest weight attribute if
   * there is any, or a non annotated class otherwise.
   *
   * @param iterator contains the objects to select from
   * @return the preferred instance
   */
  public T select(Iterator<T> iterator) {
    T preferred = null;

    if (iterator.hasNext()) {
      preferred = iterator.next();

      while (iterator.hasNext()) {
        T current = iterator.next();

        if (comparator.compare(preferred, current) == -1) {
          preferred = current;
        }
      }
    }

    return preferred;
  }
}
