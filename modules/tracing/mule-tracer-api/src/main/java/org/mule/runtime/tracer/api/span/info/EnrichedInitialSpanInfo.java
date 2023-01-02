/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.api.span.info;

/**
 * An enriched {@link InitialSpanInfo} with additional information apart from a base initial span info.
 *
 * @since 4.5.0
 */
public interface EnrichedInitialSpanInfo extends InitialSpanInfo {

  /**
   * @return the base {@link InitialSpanInfo}.
   */
  InitialSpanInfo getBaseInitialSpanInfo();

}
