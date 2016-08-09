/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import java.time.LocalDateTime;
import java.util.function.BiFunction;

/**
 * A Boolean {@link BiFunction} which returns {@code true} if a given {@link LocalDateTime} is posterior or equal to a reference
 * date
 *
 * @since 4.0
 */
public final class TimeSinceFunction implements BiFunction<LocalDateTime, LocalDateTime, Boolean> {

  /**
   * @param criteria the reference value
   * @param value the value to be tested
   * @return {@code true} if {@code value} is posterior or equal to {@code criteria}
   */
  @Override
  public Boolean apply(LocalDateTime criteria, LocalDateTime value) {
    return value.compareTo(criteria) >= 0;
  }
}
