/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.api.context.getter;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;

import java.util.Optional;

/**
 * Interface that allows to read trace context fields from a carrier, the medium that is used for carrying them.
 *
 * @since 4.5.0
 */
public interface DistributedTraceContextGetter {

  /**
   * Returns all the keys of the carrier.
   *
   * @return the keys of the carrier.
   */
  Iterable<String> keys();

  /**
   * Returns the value of a field in a carrier.
   *
   * @param key the key of the field.
   *
   * @return optional the value of the propagated field.
   */
  Optional<String> get(String key);

  /**
   * @return an empty {@link DistributedTraceContextGetter}
   */
  static DistributedTraceContextGetter emptyTraceContextMapGetter() {
    return new DistributedTraceContextGetter() {

      @Override
      public Iterable<String> keys() {
        return emptyList();
      }

      @Override
      public Optional<String> get(String key) {
        return empty();
      }
    };
  }

  default boolean isEmptyDistributedTraceContext() {
    return false;
  }
}
