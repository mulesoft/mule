/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
