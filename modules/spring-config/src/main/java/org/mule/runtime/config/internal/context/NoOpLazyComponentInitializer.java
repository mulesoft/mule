/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.context;

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
