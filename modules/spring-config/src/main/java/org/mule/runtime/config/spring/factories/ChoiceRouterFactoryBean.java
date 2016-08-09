/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.factories;

import org.mule.runtime.core.routing.AbstractSelectiveRouter;
import org.mule.runtime.core.routing.ChoiceRouter;

public class ChoiceRouterFactoryBean extends AbstractSelectiveRouterFactoryBean {

  public Class<?> getObjectType() {
    return ChoiceRouter.class;
  }

  @Override
  protected AbstractSelectiveRouter newAbstractSelectiveRouter() {
    return new ChoiceRouter();
  }
}
