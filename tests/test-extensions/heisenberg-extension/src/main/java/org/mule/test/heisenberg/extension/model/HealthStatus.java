/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension.model;

//TODO : MULE-13189 - EL returns String instead of Enum instance when accessing a variable
public enum HealthStatus {
  HEALTHY("HEALTHY"), CANCER("CANCER"), DEAD("DEAD");

  private String id;

  HealthStatus(String id) {
    this.id = id;
  }

  public String toString() {
    return id;
  }

}
