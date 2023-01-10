/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
