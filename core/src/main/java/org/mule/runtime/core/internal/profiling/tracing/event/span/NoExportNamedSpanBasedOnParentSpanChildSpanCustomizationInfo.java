/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.profiling.tracing.SpanCustomizationInfo;

/**
 * A {@link NamedSpanBasedOnParentSpanChildSpanCustomizationInfo} to not export the span.
 *
 * @since 4.5.0
 */
public class NoExportNamedSpanBasedOnParentSpanChildSpanCustomizationInfo
    extends NamedSpanBasedOnParentSpanChildSpanCustomizationInfo {

  private static final SpanCustomizationInfo INSTANCE = new NoExportNamedSpanBasedOnParentSpanChildSpanCustomizationInfo();

  public static final SpanCustomizationInfo getNoExportChildNamedSpanBasedOnParentSpanChildSpanCustomizationInfo() {
    return INSTANCE;
  }

  private NoExportNamedSpanBasedOnParentSpanChildSpanCustomizationInfo() {}

  @Override
  public boolean isExportable(CoreEvent coreEvent) {
    return false;
  }
}
