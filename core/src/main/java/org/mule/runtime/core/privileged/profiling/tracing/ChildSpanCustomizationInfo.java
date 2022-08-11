/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.privileged.profiling.tracing;

import org.mule.runtime.core.internal.profiling.tracing.event.span.NamedSpanBasedOnParentSpanChildSpanCustomizationInfo;

/**
 * A {@link ChildSpanCustomizationInfo} that has additional information on how the children spans of a span have to be created. It
 * can be used or ignored by the {@link SpanCustomizationInfo}.
 *
 * @see {@link NamedSpanBasedOnParentSpanChildSpanCustomizationInfo}
 *
 * @since 4.5.0
 */
public interface ChildSpanCustomizationInfo {

  /**
   * @return the child span suggested name.
   */
  String getChildSpanSuggestedName();

  /**
   * @return a default {@link ChildSpanCustomizationInfo}.
   */
  static ChildSpanCustomizationInfo getDefaultChildSpanInfo() {
    return () -> "";
  }
}
