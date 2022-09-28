/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span;

import org.mule.runtime.core.api.event.CoreEvent;

import static org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanUtils.getComponentNameWithoutNamespace;

/**
 * A {@link NamedSpanBasedOnParentSpanChildSpanCustomizationInfo} that does not export the children of execute next.
 *
 * @since 4.5.0
 */
public class NoExportExecuteNextChildSpanCustomizationInfo extends NamedSpanBasedOnParentSpanChildSpanCustomizationInfo {

  @Override
  public boolean isExportable(CoreEvent coreEvent) {
    return getSpan(coreEvent)
        .map(internalSpan -> !getComponentNameWithoutNamespace(internalSpan).equals(EXECUTE_NEXT_ROUTE_TAG)).orElse(true);
  }

}
