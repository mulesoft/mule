/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.config.api.LazyComponentInitializer;

/**
 * NoOp implementation for {@link LazyComponentInitializer}.
 */
public class NoOpLazyComponentInitializer implements LazyComponentInitializer {

  @Override
  public void initializeComponents(ComponentLocationFilter componentLocationFilter) {
    // Nothing to do...
  }

  @Override
  public void initializeComponent(Location location) {
    // Nothing to do...
  }

}
