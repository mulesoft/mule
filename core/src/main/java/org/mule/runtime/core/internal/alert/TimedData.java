/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.alert;

import java.time.Instant;

public class TimedData<T> {

  private Instant time;
  private T data;

  public TimedData(Instant time, T data) {
    this.time = time;
    this.data = data;
  }

  public Instant getTime() {
    return time;
  }

  public T getData() {
    return data;
  }

}
