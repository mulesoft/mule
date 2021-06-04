/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.management.execution;

import org.mule.runtime.core.api.event.CoreEvent;

/**
 * An @{@link ProcessingStrategyExecutionProfiler} that does not perform any action.
 *
 * @since 4.4.0, 4.3.1
 */
public class DefaultProcessingStrategyExecutionProfiler implements ProcessingStrategyExecutionProfiler {

  private static ProcessingStrategyExecutionProfiler instance = new DefaultProcessingStrategyExecutionProfiler();

  private DefaultProcessingStrategyExecutionProfiler() {}

  public static ProcessingStrategyExecutionProfiler getInstance() {
    return instance;
  }

  @Override
  public void profileBeforeDispatchingToProcessor(CoreEvent e) {
    // Nothing to do
  }

  @Override
  public void profileBeforeComponentProcessing(CoreEvent e) {
    // Nothing to do
  }

  @Override
  public void profileAfterResponseReceived(CoreEvent e) {
    // Nothing to do
  }

  @Override
  public void profileAfterDispatchingToFlow(CoreEvent e) {
    // Nothing to do
  }
}
