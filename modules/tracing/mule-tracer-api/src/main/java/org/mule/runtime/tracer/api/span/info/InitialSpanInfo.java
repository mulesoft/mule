/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.api.span.info;

import org.mule.runtime.tracer.api.span.InternalSpan;

import static java.util.Collections.emptyMap;

import java.util.Map;

/**
 * Initial info for a starting an {@link InternalSpan}.
 *
 * @since 4.5.0
 */
public interface InitialSpanInfo {

  /**
   * @return the initial name for the span.
   */
  String getName();

  /**
   * @return initial attributes for the span.
   */
  default Map<String, String> getInitialAttributes() {
    return emptyMap();
  }

  /**
   * @return indicates that the {@link InternalSpan} belongs to a policy. TODO: Technical debt: verify order of spans in the case
   *         of policies (W-12041739)
   */
  default boolean isPolicySpan() {
    return false;
  }

  /**
   * indicates if it is the first entry point of a mule pan. For example if there is a policy and some attributes are propagated
   * from a connector, this will be added to the flow span and not to the policy spans.
   *
   * @return indicates if it is the first entry point of a mule pan.
   *
   */
  default boolean isRootSpan() {
    return false;
  }

  /**
   * @return initial information concerning the export of the span.
   */
  StartExportInfo getStartExportInfo();
}
