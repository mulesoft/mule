/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.infrastructure;

import org.mule.runtime.core.time.Time;

/**
 * Contains information about how much time should a dynamic config be idle before it can be considered elegible for expiration
 *
 * @since 4.0
 */
public class DynamicConfigurationExpiration {

  private final Time frequency;

  public DynamicConfigurationExpiration(Time frequency) {
    this.frequency = frequency;
  }

  public Time getFrequency() {
    return frequency;
  }
}
