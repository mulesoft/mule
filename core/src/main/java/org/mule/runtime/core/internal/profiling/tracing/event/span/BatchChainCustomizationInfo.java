/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.tracing.event.span;

import static org.mule.runtime.core.privileged.profiling.tracing.ChildSpanCustomizationInfo.getDefaultChildSpanInfo;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.profiling.tracing.ChildSpanCustomizationInfo;

/**
 * Batch Chain Customization info. Includes batch-step-record, batch-aggregator and batch-on-complete spans.
 *
 * @since 4.5.0
 */
public class BatchChainCustomizationInfo extends AbstractDefaultAttributesResolvingSpanCustomizationInfo {

  private final String name;
  private ComponentLocation location;

  public BatchChainCustomizationInfo(String name, ComponentLocation location) {
    this.name = name;
    this.location = location;
  }

  @Override
  public String getName(CoreEvent coreEvent) {
    return name;
  }

  @Override
  public ChildSpanCustomizationInfo getChildSpanCustomizationInfo() {
    return getDefaultChildSpanInfo();
  }

  @Override
  public String getLocationAsString(CoreEvent coreEvent) {
    return CoreEventSpanUtils.getLocationAsString(location);
  }
}
