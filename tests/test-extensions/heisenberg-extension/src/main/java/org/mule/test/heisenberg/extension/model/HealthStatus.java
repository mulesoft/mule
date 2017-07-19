/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension.model;

public enum HealthStatus {
  HEALTHY("Healthy"), CANCER("Cancer"), DEAD("Dead");

  private String id;

  HealthStatus(String id) {
    this.id = id;
  }

  public String toString() {
    return id;
  }

}
