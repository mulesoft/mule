/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lifecycle.phases;

import static java.util.Collections.emptyList;

import org.mule.runtime.api.lifecycle.Initialisable;

import java.util.List;

/**
 * This lifecycle phase marks the 'pre-lifecycle' phase of an object. The default phase before any other phase has been executed
 */
public class NotInLifecyclePhase extends DefaultLifecyclePhase {

  public static final String PHASE_NAME = "not in lifecycle";

  public NotInLifecyclePhase() {
    super(PHASE_NAME, null);
    registerSupportedPhase(Initialisable.PHASE_NAME);
  }

  @Override
  public LifecycleObjectSorter newLifecycleObjectSorter() {
    return new LifecycleObjectSorter() {

      @Override
      public void addObject(String name, Object object) {

      }

      @Override
      public List<Object> getSortedObjects() {
        return emptyList();
      }

    };
  }
}
