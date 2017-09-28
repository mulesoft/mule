/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleRuntimeException;

/**
 * Creator for configuration resources.
 */
public interface LazyComponentCreator {

  /**
   * Calling this method guarantees that the requested component from the configuration will be created.
   * <p/>
   * The requested component must exists in the configuration.
   *
   * @param location the location of the configuration component.
   * @throws MuleRuntimeException if there's a problem creating the component or the component does not exists.
   */
  void createComponent(Location location);

}
