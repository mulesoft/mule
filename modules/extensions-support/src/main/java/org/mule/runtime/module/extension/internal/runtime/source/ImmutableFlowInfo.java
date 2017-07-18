/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.runtime.extension.api.runtime.FlowInfo;

/**
 * Immutable implementation of {@link FlowInfo}
 *
 * @since 4.0
 */
public class ImmutableFlowInfo implements FlowInfo {

  private final String name;
  private final int maxConcurrency;

  public ImmutableFlowInfo(String name, int maxConcurrency) {
    this.name = name;
    this.maxConcurrency = maxConcurrency;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getMaxConcurrency() {
    return maxConcurrency;
  }
}
