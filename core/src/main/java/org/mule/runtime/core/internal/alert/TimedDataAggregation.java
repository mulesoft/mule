/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.alert;


public class TimedDataAggregation<A> {

  private A agg1;
  private A agg5;
  private A agg15;

  public TimedDataAggregation(A agg1, A agg5, A agg15) {
    this.agg1 = agg1;
    this.agg5 = agg5;
    this.agg15 = agg15;
  }

  public A forLast1MinInterval() {
    return agg1;
  }

  public A forLast5MinsInterval() {
    return agg5;
  }

  public A forLast15MinsInterval() {
    return agg15;
  }

  @Override
  public String toString() {
    return agg1 + ", " + agg5 + ", " + agg15;
  }
}
