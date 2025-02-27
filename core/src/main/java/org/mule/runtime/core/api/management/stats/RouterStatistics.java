/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.management.stats;

import org.mule.api.annotation.NoExtend;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <code>RouterStatistics</code> TODO
 *
 */
@NoExtend
public class RouterStatistics implements Statistics {

  /**
   * Serial version
   */
  private static final long serialVersionUID = 4540482357430845065L;

  public static final int TYPE_INBOUND = 1;
  public static final int TYPE_OUTBOUND = 2;
  public static final int TYPE_RESPONSE = 3;
  public static final int TYPE_BINDING = 4;

  private boolean enabled;
  private long notRouted;
  private long caughtInCatchAll;
  private long totalRouted;
  private long totalReceived;
  private final Map<String, Long> routed;
  private final int type;

  public synchronized void clear() {
    notRouted = 0;
    totalRouted = 0;
    totalReceived = 0;
    caughtInCatchAll = 0;
    routed.clear();
  }

  /**
   * @see org.mule.runtime.core.api.management.stats.Statistics#isEnabled()
   */
  @Override
  public boolean isEnabled() {
    return enabled;
  }

  public synchronized void setEnabled(boolean b) {
    enabled = b;
  }

  /**
   * The constructor
   */
  public RouterStatistics(int type) {
    super();
    this.type = type;
    routed = new HashMap<>();
  }

  /**
   * Increment routed message for multiple targets
   *
   * @param endpoints The endpoint collection
   */
  public void incrementRoutedMessage(Collection endpoints) {
    if (endpoints == null || endpoints.isEmpty()) {
      return;
    }
    List<Object> list = new ArrayList<>(endpoints);
    synchronized (routed) {
      for (Object o : list) {
        incrementRoutedMessage(o);
      }
    }
  }

  /**
   * Increment routed message for an endpoint
   *
   * @param endpoint The endpoint
   */
  public synchronized void incrementRoutedMessage(Object endpoint) {
    if (endpoint == null) {
      return;
    }

    String name = endpoint.toString();

    Long cpt = routed.get(name);
    long count = 0;

    if (cpt != null) {
      count = cpt;
    }

    // TODO we should probably use a MutableLong here,
    // but that might be problematic for remote MBean access (serialization)
    routed.put(name, ++count);

    totalRouted++;
    totalReceived++;
  }

  /**
   * Increment no routed message
   */
  public synchronized void incrementNoRoutedMessage() {
    notRouted++;
    totalReceived++;
  }

  /**
   * Increment caught in catch all message
   */
  public synchronized void incrementCaughtMessage() {
    caughtInCatchAll++;
  }

  /**
   * @return Returns the caughtInCatchAll.
   */
  public final long getCaughtMessages() {
    return caughtInCatchAll;
  }

  /**
   * @return Returns the notRouted.
   */
  public final long getNotRouted() {
    return notRouted;
  }

  /**
   * @return Returns the totalReceived.
   */
  public final long getTotalReceived() {
    return totalReceived;
  }

  /**
   * @return Returns the totalRouted.
   */
  public final long getTotalRouted() {
    return totalRouted;
  }

  /**
   * @return Returns the totalRouted.
   */
  public final long getRouted(String endpointName) {
    Long value = routed.get(endpointName);

    if (value == null) {
      return 0;
    } else {
      return value;
    }
  }

  public boolean isInbound() {
    return type == TYPE_INBOUND;
  }

  public Map<String, Long> getRouted() {
    return routed;
  }
}
