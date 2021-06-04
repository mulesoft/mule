/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.management.pipeline;

import org.mule.runtime.core.api.event.CoreEvent;

/**
 * A {@link ProcessingStrategyPipelineProfiler} that does not perform any action.
 */
public class DefaultProcessingStrategyPipelineProfiler implements ProcessingStrategyPipelineProfiler {

  @Override
  public void profileBeforeDispatchingToPipeline(CoreEvent e) {
    // Nothing to do
  }

  @Override
  public void profileAfterPipelineProcessed(CoreEvent e) {
    // Nothing to do
  }
}
