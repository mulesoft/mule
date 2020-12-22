/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config;

import org.mule.runtime.api.config.Feature;

public enum TestingFeatures implements Feature {

  TESTING_FEATURE("Testing feature");

  private final String description;

  TestingFeatures(String description) {
    this.description = description;
  }

  @Override
  public String getDescription() {
    return description;
  }
}
