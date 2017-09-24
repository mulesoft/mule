/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing.correlation;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.core.api.event.CoreEvent;

import java.util.Comparator;
import java.util.Optional;

/**
 * <code>CorrelationSequenceComparator</code> is a {@link Comparator} for {@link CoreEvent}s using their respective correlation
 * sequences.
 */
public final class CorrelationSequenceComparator implements Comparator<CoreEvent> {

  @Override
  public int compare(CoreEvent event1, CoreEvent event2) {
    Optional<Integer> val1 = event1.getGroupCorrelation().map(gc -> of(gc.getSequence())).orElse(empty());
    Optional<Integer> val2 = event2.getGroupCorrelation().map(gc -> of(gc.getSequence())).orElse(empty());

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
