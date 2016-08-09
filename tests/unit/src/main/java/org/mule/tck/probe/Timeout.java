/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
