/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.privileged.profiling.tracing.SpanCustomizationInfo;

/**
 * A {@link NamedSpanBasedOnComponentIdentifierAloneSpanCustomizationInfo} that will not export
 * any spans from its children hierarchy unless it is overridden by a span's {@link SpanCustomizationInfo}.
 *
 * @see SpanCustomizationInfo#ignoreExportLevelLimitOfAncestors()
 *
 * @since 4.5.0
 */
public class NoExportableNamedSpanBasedOnComponentIdentifierAloneSpanCustomizationInfo extends
    NamedSpanBasedOnComponentIdentifierAloneSpanCustomizationInfo {

  public NoExportableNamedSpanBasedOnComponentIdentifierAloneSpanCustomizationInfo(
                                                                                   Component component) {
    super(component);
  }

  @Override
  public int exportUntilLevel() {
    return 1;
  }
}
