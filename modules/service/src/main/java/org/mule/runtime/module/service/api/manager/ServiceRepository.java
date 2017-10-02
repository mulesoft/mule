/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.api.manager;

import org.mule.runtime.api.service.Service;

import java.util.List;

/**
 * Provides access to the services available in the container.
 */
public interface ServiceRepository {

  /**
   * Provides access to the services available in the container.
   *
   * @return a non null list of services.
   */
  List<Service> getServices();

}
