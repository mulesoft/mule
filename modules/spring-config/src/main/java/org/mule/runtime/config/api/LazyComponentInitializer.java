/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleRuntimeException;

/**
 * Initializer for the creation of lazy resources.
 *
 * @since 4.0
 */
public interface LazyComponentInitializer {

  /**
   * Key under which the {@link LazyComponentInitializer} can be found in the {@link org.mule.runtime.api.artifact.Registry}
   */
  String LAZY_COMPONENT_INITIALIZER_SERVICE_KEY = "_muleLazyComponentInitializer";

  /**
   * Calling this method guarantees that the components accepted by the filter from the configuration will be created and
   * initialized.
   * <p/>
   * If there were any component already initialized it will be unregistered in order to initialize the ones selected by this filter.
   *
   * @param componentLocationFilter {@link ComponentLocationFilter} to select the {@link Component} to be initialized and they dependent ones.
   * @throws MuleRuntimeException if there's a problem creating the component or the component does not exists.
   */
  void initializeComponents(ComponentLocationFilter componentLocationFilter);

  /**
   * Calling this method guarantees that the requested component from the configuration will be created and
   * initialized.
   * <p/>
   * The requested component must exists in the configuration. If there was a component already initialized it will be unregistered
   * in order to initialize the requested component and its dependencies.
   *
   * @param location the location of the configuration component.
   * @throws MuleRuntimeException if there's a problem creating the component or the component does not exists.
   */
  void initializeComponent(Location location);

  /**
   * Defines which {@link Component components} should be initialized by accepting it by {@link ComponentLocation}.
   *
   * @since 4.0
   */
  interface ComponentLocationFilter {

    /**
     * @param componentLocation {@link ComponentLocation} of a {@link Component} in configuration.
     * @return {@code true} if the {@link Component} associated to the {@link ComponentLocation} should be initialized.
     */
    boolean accept(ComponentLocation componentLocation);

  }

}
