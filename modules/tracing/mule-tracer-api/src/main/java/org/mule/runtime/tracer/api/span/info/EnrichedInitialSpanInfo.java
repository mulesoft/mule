/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
