/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.locator;

import org.mule.runtime.api.component.location.Location;

import java.util.Optional;

/**
 * Locator to access runtime objects created from the configuration of the artifact.
 *
 * The location can be composed by many parts, each part separated by an slash.
 *
 * The first part must be the name of the global element that contains the location. Location "myflow" indicates the global
 * element with name myFlow.
 *
 * Global elements that do not have a name cannot be referenced.
 *
 * The following parts must be components contained within the global element. Location "myFlow/processors" indicates the
 * processors part of the global element with name "myFlow"
 *
 * Each part must be contained in the preceded component in the location. Location "myFlow/errorHandler/onErrors" indicates the
 * onErrors components that are part of the errorHandler component which is also part of the "myFlow" global element.
 *
 * When a component part has a collection of components, each component can be referenced individually with an index. THe first
 * index is 0. Location "myFlow/processors/4" refers to the fifth processors inside the flow with name "myFlow"
 * 
 * @since 4.0
 */
public interface ConfigurationComponentLocator {

  /**
   * Finds a component in the configuration with the specified location. Only simple objects locations are accepted.
   *
   * @param location the location of the component.
   * @return the found component or empty if there's no such component.
   */
  Optional<Object> find(Location location);

}
