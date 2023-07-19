/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.exporter.impl.optel.resources;

import java.io.IOException;

/**
 * An Exception raised during configuration.
 *
 * @since 4.5.0
 */
public class SpanExporterConfiguratorException extends RuntimeException {

  public SpanExporterConfiguratorException(Exception e) {
    super(e);
  }

  public SpanExporterConfiguratorException(String s) {
    super(s);
  }
}
