/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
