/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tests.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LifecycleTrackerRegistry {

  private final Map<String, LifecycleTracker> trackers = new ConcurrentHashMap<>();

  public LifecycleTracker get(String trackerName) {
    return trackers.get(trackerName);
  }

  public void add(String trackerName, LifecycleTracker tracker) {
    trackers.put(trackerName, tracker);
  }
}
