/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.impl;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.tracer.api.span.info.EnrichedInitialSpanInfo;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;

public class SpanInfoUtils {

  public static EnrichedInitialSpanInfo enrichInitialSpanInfo(InitialSpanInfo spanCustomizationInfo, CoreEvent coreEvent) {
    return new CoreEventEnrichedInitialSpanInfo(spanCustomizationInfo, coreEvent);
  }

}
