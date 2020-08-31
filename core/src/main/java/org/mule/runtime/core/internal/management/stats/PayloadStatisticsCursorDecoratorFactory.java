/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import static org.mule.runtime.core.internal.management.stats.NoOpCursorComponentDecoratorFactory.NO_OP_INSTANCE;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.management.stats.AllStatistics;
import org.mule.runtime.core.api.management.stats.CursorComponentDecoratorFactory;

public class PayloadStatisticsCursorDecoratorFactory implements MuleContextAware, CursorDecoratorFactory {

  // TODO MULE-18648 @Inject this
  private AllStatistics statistics;

  // TODO MULE-18648 remove this
  @Override
  public void setMuleContext(MuleContext context) {
    statistics = context.getStatistics();
  }

  @Override
  public CursorComponentDecoratorFactory componentDecoratorFactory(Component component) {
    if (component.getLocation() != null) {
      return new PayloadStatisticsCursorComponentDecoratorFactory(statistics.computePayloadStatisticsIfAbsent(component));
    } else {
      return NO_OP_INSTANCE;
    }
  }
}
