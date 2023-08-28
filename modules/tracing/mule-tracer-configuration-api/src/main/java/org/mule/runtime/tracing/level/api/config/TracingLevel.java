/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracing.level.api.config;

/**
 * Set of hierarchical levels that group and define different types of generation of traces.
 *
 * @since 4.5.0
 */
public enum TracingLevel {
  /**
   * Overview: Generates only traces for the Flows and the Inbound and Outbound endpoints of the app.
   */
  OVERVIEW,
  /**
   * Monitoring: Same behavior as Overview, plus, traces for each 'box' or step of an application.
   */
  MONITORING,
  /**
   * Debug: Same behavior as Monitoring, plus, additional information inside the connectors and components.
   */
  DEBUG
}
