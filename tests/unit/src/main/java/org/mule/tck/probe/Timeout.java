/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.probe;

public class Timeout {

  private final long duration;
  private final long start;

  public Timeout(long duration) {
    this.duration = duration;
    this.start = System.currentTimeMillis();
  }

  public boolean hasTimedOut() {
    final long now = System.currentTimeMillis();
    return (now - start) > duration;
  }
}
