/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.spring;

import org.mule.runtime.core.api.CustomizationService;

/**
 * Enables to customize a {@link org.mule.runtime.core.api.MuleContext} by using the corresponding {@link CustomizationService}
 */
public interface MuleContextServiceConfigurator {

  /**
   * Configures services for the associated {@link org.mule.runtime.core.api.MuleContext}.
   *
   * @param customizationService used to configure the provided services. Non null.
   */
  void configure(CustomizationService customizationService);
}
