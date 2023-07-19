/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metrics.exporter.impl.utils;

/**
 * Encapsulates the data of an exported meter.
 */
public class TestExportedMeter {

  private String resourceName;
  private String description;
  private String instrumentName;
  private String name;
  private long value;

  public void setResourceName(String resourceName) {
    this.resourceName = resourceName;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setInstrumentName(String instrumentName) {
    this.instrumentName = instrumentName;
  }

  public String getResourceName() {
    return resourceName;
  }

  public String getDescription() {
    return description;
  }

  public String getInstrumentName() {
    return instrumentName;
  }

  public String getName() {
    return name;
  }

  public long getValue() {
    return value;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setValue(long value) {
    this.value = value;
  }
}
