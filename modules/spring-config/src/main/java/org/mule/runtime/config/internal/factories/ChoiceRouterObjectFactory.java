/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import org.mule.runtime.core.internal.routing.AbstractSelectiveRouter;
import org.mule.runtime.core.internal.routing.ChoiceRouter;

public class ChoiceRouterObjectFactory extends AbstractSelectiveRouterObjectFactory {

  public Class<?> getObjectType() {
    return ChoiceRouter.class;
  }

  @Override
  protected AbstractSelectiveRouter newAbstractSelectiveRouter() {
    return new ChoiceRouter();
  }
}
