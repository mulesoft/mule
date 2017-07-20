/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing.correlation;

import org.mule.runtime.core.api.Event;

import java.util.Comparator;
import java.util.Optional;

/**
 * <code>CorrelationSequenceComparator</code> is a {@link Comparator} for {@link Event}s using their respective correlation
 * sequences.
 */
public final class CorrelationSequenceComparator implements Comparator<Event> {

  @Override
  public int compare(Event event1, Event event2) {
    Optional<Integer> val1 = event1.getGroupCorrelation().getSequence();
    Optional<Integer> val2 = event2.getGroupCorrelation().getSequence();

    if (val1 == val2) {
      return 0;
    } else if (val1.isPresent() && !val2.isPresent()) {
      return 1;
    } else if (!val1.isPresent() && val2.isPresent()) {
      return -1;
    } else {
      return val1.get().compareTo(val2.get());
    }
  }
}
