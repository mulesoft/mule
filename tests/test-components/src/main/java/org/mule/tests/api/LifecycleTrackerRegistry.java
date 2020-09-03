/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tests.api;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LifecycleTrackerRegistry {

  private final Map<String, Collection<String>> phasesByTracker = new ConcurrentHashMap<>();

  public Collection<String> get(String trackerName) {
    return phasesByTracker.get(trackerName);
  }

  public void add(String trackerName, Collection<String> phases) {
    phasesByTracker.put(trackerName, phases);
  }
}
