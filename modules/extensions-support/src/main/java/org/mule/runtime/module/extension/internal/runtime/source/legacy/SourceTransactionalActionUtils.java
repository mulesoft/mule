/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source.legacy;

import static java.lang.String.format;

import org.mule.sdk.api.tx.SourceTransactionalAction;

/**
 * Utils class for handling {@link SourceTransactionalAction}
 *
 * @since 4.5.0
 */
public final class SourceTransactionalActionUtils {

  private SourceTransactionalActionUtils() {}

  /**
   * Returns the assosiated {@link SourceTransactionalAction} from the given value. The given value must either be an
   * {@link SourceTransactionalAction} or an {@link org.mule.sdk.api.tx.SourceTransactionalAction}
   *
   * @param sourceTransactionalAction the value to take the {@link SourceTransactionalAction}
   * @return the {@link SourceTransactionalAction} associated to the given argument.
   */
  public static SourceTransactionalAction from(Object sourceTransactionalAction) {
    if (sourceTransactionalAction instanceof SourceTransactionalAction) {
      return (SourceTransactionalAction) sourceTransactionalAction;
    } else if (sourceTransactionalAction instanceof org.mule.runtime.extension.api.tx.SourceTransactionalAction) {
      return fromLegacy((org.mule.runtime.extension.api.tx.SourceTransactionalAction) sourceTransactionalAction);
    }
    throw new IllegalArgumentException(format("SourceTransactionalAction is expected to be a org.mule.sdk.api.tx.SourceTransactionalAction or org.mule.runtime.extension.api.tx.SourceTransactionalAction, but was %s",
                                              sourceTransactionalAction.getClass()));
  }

  /**
   * Returns the assosiated {@link SourceTransactionalAction} from the given value. The given value must either be an
   * {@link SourceTransactionalAction} or an {@link org.mule.sdk.api.tx.SourceTransactionalAction}
   *
   * @param sourceTransactionalAction the value to take the {@link SourceTransactionalAction}
   * @return the {@link SourceTransactionalAction} associated to the given argument.
   */
  public static org.mule.runtime.extension.api.tx.SourceTransactionalAction toLegacy(Object sourceTransactionalAction) {
    if (sourceTransactionalAction instanceof org.mule.runtime.extension.api.tx.SourceTransactionalAction) {
      return (org.mule.runtime.extension.api.tx.SourceTransactionalAction) sourceTransactionalAction;
    } else if (sourceTransactionalAction instanceof SourceTransactionalAction) {
      return toLegacy((SourceTransactionalAction) sourceTransactionalAction);
    }
    throw new IllegalArgumentException(format("SourceTransactionalAction is expected to be a org.mule.sdk.api.tx.SourceTransactionalAction or org.mule.runtime.extension.api.tx.SourceTransactionalAction, but was %s",
                                              sourceTransactionalAction.getClass()));
  }

  private static org.mule.runtime.extension.api.tx.SourceTransactionalAction toLegacy(SourceTransactionalAction sourceTransactionalAction) {
    switch (sourceTransactionalAction) {
      case ALWAYS_BEGIN:
        return org.mule.runtime.extension.api.tx.SourceTransactionalAction.ALWAYS_BEGIN;
      case NONE:
        return org.mule.runtime.extension.api.tx.SourceTransactionalAction.NONE;
    }
    return null;
  }

  private static SourceTransactionalAction fromLegacy(org.mule.runtime.extension.api.tx.SourceTransactionalAction sourceTransactionalAction) {
    switch (sourceTransactionalAction) {
      case ALWAYS_BEGIN:
        return org.mule.sdk.api.tx.SourceTransactionalAction.ALWAYS_BEGIN;
      case NONE:
        return org.mule.sdk.api.tx.SourceTransactionalAction.NONE;
    }
    return null;
  }
}
