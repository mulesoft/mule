/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracing.level.api.config;

/**
 * Set of hierarchical levels that group and define different types of generation of traces.
 *
 * @since 4.5.0
 */
public enum TracingLevelId {
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
