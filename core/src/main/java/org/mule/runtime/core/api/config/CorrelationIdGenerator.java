/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.config;

/**
 * Generator of correlation IDs of events.
 *
 * @Since 4.4.0
 */
public interface CorrelationIdGenerator {

  /**
   * @return a string representing the correlation id to be used
   */
  String generateCorrelationId();

}
