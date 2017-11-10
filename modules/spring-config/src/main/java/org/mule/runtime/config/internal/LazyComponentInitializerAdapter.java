/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.config.api.LazyComponentInitializer;

/**
 * Initializer for the creation of lazy resources.
 * <p>
 * Adds internal behaviour to be able to control the initialization of components.
 *
 * @since 4.0.1
 */
public interface LazyComponentInitializerAdapter extends LazyComponentInitializer {

  /**
   * Calling this method guarantees that the requested component from the configuration will be created and
   * initialized.
   * <p/>
   * The requested component must exists in the configuration. If there was a component already initialized it will be unregistered
   * in order to initialize the requested component and its dependencies.
   *
   * @param location the location of the configuration component.
   * @param applyStartPhase boolean indicating if the Start phase should be applied to the created components
   * @throws MuleRuntimeException if there's a problem creating the component or the component does not exists.
   */
  void initializeComponent(Location location, boolean applyStartPhase);

  /**
   * Calling this method guarantees that the components accepted by the filter from the configuration will be created and
   * initialized.
   * <p/>
   * If there were any component already initialized it will be unregistered in order to initialize the ones selected by this filter.
   *
   * @param componentLocationFilter {@link ComponentLocationFilter} to select the {@link Component} to be initialized and they dependent ones.
   * @param applyStartPhase boolean indicating if the Start phase should be applied to the created components
   * @throws MuleRuntimeException if there's a problem creating the component or the component does not exists.
   */
  void initializeComponents(ComponentLocationFilter componentLocationFilter, boolean applyStartPhase);
}
