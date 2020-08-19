/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.util;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.management.stats.AllStatistics;

import java.util.function.Supplier;

/**
 * Ensures the test containing the rule runs with statistics enabled.
 * <p>
 * Ref: {@link AllStatistics#setEnabled(boolean)}.
 *
 * @since 4.4, 4.3.1
 */
public class StatisticsEnabled {

  private final Supplier<MuleContext> muleContext;

  private boolean oldValue;

  public StatisticsEnabled(Supplier<MuleContext> muleContext) {
    this.muleContext = muleContext;
  }

  public void before() throws Throwable {
    oldValue = muleContext.get().getStatistics().isEnabled();
    muleContext.get().getStatistics().setEnabled(true);
  }

  public void after() {
    muleContext.get().getStatistics().setEnabled(oldValue);
  }
}
