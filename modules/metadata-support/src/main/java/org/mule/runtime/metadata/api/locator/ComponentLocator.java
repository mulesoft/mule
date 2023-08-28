/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metadata.api.locator;

import org.mule.runtime.api.component.location.Location;

import java.util.Optional;

@FunctionalInterface
public interface ComponentLocator<C> {

  /**
   * @param location the location of a Component
   * @return the Component present at the given {@link Location}, or {@link Optional#empty()} if none was found.
   */
  Optional<C> get(Location location);

}
