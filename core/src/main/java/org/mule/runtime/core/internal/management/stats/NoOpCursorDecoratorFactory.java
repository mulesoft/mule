/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.management.stats.CursorComponentDecoratorFactory;

import static org.mule.runtime.core.internal.management.stats.NoOpCursorComponentDecoratorFactory.NO_OP_INSTANCE;

public class NoOpCursorDecoratorFactory implements CursorDecoratorFactory {

  @Override
  public CursorComponentDecoratorFactory componentDecoratorFactory(Component component) {
    return NO_OP_INSTANCE;
  }
}
