/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.common.api.matcher;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.util.Optional;

/**
 * Criterias used to accept or reject a matcher filter.
 *
 * @since 1.0
 */
public enum MatchPolicy {

  /**
   * Accept only the files which comply with the filter
   */
  ONLY {

    @Override
    public Optional<Boolean> asBoolean() {
      return of(Boolean.TRUE);
    }

    @Override
    public boolean acceptsAll() {
      return false;
    }
  },

  /**
   * Accept all files
   */
  ACCEPTS {

    @Override
    public Optional<Boolean> asBoolean() {
      return empty();
    }

    @Override
    public boolean acceptsAll() {
      return true;
    }
  },
  /**
   * Accept only the files which do not match the filter
   */
  REJECTS {

    @Override
    public Optional<Boolean> asBoolean() {
      return of(Boolean.FALSE);
    }

    @Override
    public boolean acceptsAll() {
      return false;
    }
  };

  /**
   * @return the boolean value associated to each option
   */
  public abstract Optional<Boolean> asBoolean();

  /**
   * @return Whether {@code this} is {@link #ACCEPTS}
   */
  public abstract boolean acceptsAll();
}
