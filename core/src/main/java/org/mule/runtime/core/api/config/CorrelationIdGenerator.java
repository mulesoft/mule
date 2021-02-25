/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
