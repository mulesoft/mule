/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
