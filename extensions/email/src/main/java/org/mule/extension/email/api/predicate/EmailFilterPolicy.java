/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.predicate;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.util.Optional;

/**
 * Criteria used to accept or reject an email filtering flag.
 *
 * @since 1.0
 */
public enum EmailFilterPolicy {

  /**
   * Accept only the emails which comply with the filter
   */
  REQUIRE {

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
   * Accept all emails whether or not they 
   */
  INCLUDE {

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
   * Accept only the emails which do not match the filter
   */
  EXCLUDE {

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
   * @return Whether {@code this} is {@link #INCLUDE}
   */
  public abstract boolean acceptsAll();
}
