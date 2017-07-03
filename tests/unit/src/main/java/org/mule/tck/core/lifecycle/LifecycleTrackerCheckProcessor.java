/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.core.lifecycle;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;

/**
 * Specialization of {@link LifecycleTrackerProcessor} that validates the phase transition being done on this component.
 * 
 * @since 4.0
 */
public class LifecycleTrackerCheckProcessor extends LifecycleTrackerProcessor {

  private final List<String> tracker = new ArrayList<String>() {

    @Override
    public boolean add(String phase) {
      if (isValidTransition(phase)) {
        return super.add(phase);
      }
      throw new IllegalStateException(format("Invalid phase transition: %s -> %s", this.toString(), phase));
    }

    private boolean isValidTransition(String phase) {
      // just check if the same phase was already invoked
      return !this.contains(phase);
    }
  };

  @Override
  public List<String> getTracker() {
    return tracker;
  }

}


