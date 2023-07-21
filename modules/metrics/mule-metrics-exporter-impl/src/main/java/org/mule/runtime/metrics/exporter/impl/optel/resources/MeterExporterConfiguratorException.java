/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metrics.exporter.impl.optel.resources;

/**
 * An Exception raised during configuration.
 *
 * @since 4.5.0
 */
public class MeterExporterConfiguratorException extends RuntimeException {

  public MeterExporterConfiguratorException(Exception e) {
    super(e);
  }

  public MeterExporterConfiguratorException(String s) {
    super(s);
  }
}
