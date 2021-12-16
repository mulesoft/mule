/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.registry;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.lifecycle.LifecycleInterceptor;

import org.springframework.context.ApplicationContext;

public class BaseSpringRegistry extends AbstractSpringRegistry {

  public BaseSpringRegistry(ApplicationContext applicationContext,
                            MuleContext muleContext,
                            LifecycleInterceptor lifecycleInterceptor) {
    super(applicationContext, muleContext, lifecycleInterceptor);
  }

}
