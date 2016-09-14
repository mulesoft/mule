/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

/**
 * Initializer for the creation of lazy resources.
 *
 * @since 4.0
 */
public interface LazyComponentInitializer {

  /**
   * Calling this method guarantees that the requested component from the configuration
   * will be created.
   *
   * @param componentIdentifier the identier of the configuration component.
   */
  void initializeComponent(String componentIdentifier);

}
