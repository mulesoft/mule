/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
/**
 * Definitions for Mule Metrics Exporter.
 *
 * @moduleGraph
 * @since 4.5
 */
module org.mule.runtime.metrics.exporter.api {

  requires org.mule.runtime.metrics.api;
  requires org.mule.runtime.metrics.exporter.configuration.api;

  exports org.mule.runtime.metrics.exporter.api;

}
