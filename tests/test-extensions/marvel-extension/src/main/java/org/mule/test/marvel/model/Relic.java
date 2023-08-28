/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.marvel.model;

public class Relic {

  private Object description;

  public Relic(Object description) {
    this.description = description;
  }

  public Relic() {}

  public Object getDescription() {
    return description;
  }

  public void setDescription(Object description) {
    this.description = description;
  }
}
