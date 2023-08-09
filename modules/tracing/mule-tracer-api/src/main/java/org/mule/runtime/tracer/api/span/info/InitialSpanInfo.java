/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.api.span.info;

import static java.util.Collections.emptyMap;

import org.mule.runtime.api.profiling.tracing.Span;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Initial info for a starting an {@link Span}.
 *
 * @since 4.5.0
 */
public interface InitialSpanInfo {

  /**
   * @return the initial name for the span.
   */
  String getName();

  /**
   * @return indicates that the {@link Span} belongs to a policy.
   */
  default boolean isPolicySpan() {
    return false;
  }

  // TODO: Technical debt: verify order of spans in the case of policies (W-12041739)
  /**
   * @return initial attributes for the span.
   */
  default Map<String, String> getInitialAttributes() {
    return emptyMap();
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
  InitialExportInfo getInitialExportInfo();

  /**
   * An operation to apply on each of the attributes
   *
   * @param biConsumer the operation to apply.
   */
  default void forEachAttribute(BiConsumer<String, String> biConsumer) {
    getInitialAttributes().forEach(biConsumer);
  }

  /**
   * @return count of the initial attributes.
   */
  default int getInitialAttributesCount() {
    return 0;
  }
}
