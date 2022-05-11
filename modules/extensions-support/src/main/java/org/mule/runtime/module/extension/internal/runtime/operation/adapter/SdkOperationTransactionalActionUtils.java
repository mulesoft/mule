/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation.adapter;

import static java.lang.String.format;

import org.mule.sdk.api.tx.OperationTransactionalAction;

/**
 * Utils class for handling {@link OperationTransactionalAction}
 *
 * @since 4.5.0
 */
public final class SdkOperationTransactionalActionUtils {

  private SdkOperationTransactionalActionUtils() {}

  /**
   * Returns the assosiated {@link OperationTransactionalAction} from the given value. The given value must either be an
   * {@link OperationTransactionalAction} or an {@link org.mule.runtime.extension.api.tx.OperationTransactionalAction}
   *
   * @param operationTransactionalAction the value to take the {@link OperationTransactionalAction}
   * @return the {@link OperationTransactionalAction} associated to the given argument.
   */
  public static OperationTransactionalAction from(Object operationTransactionalAction) {
    if (operationTransactionalAction instanceof OperationTransactionalAction) {
      return (OperationTransactionalAction) operationTransactionalAction;
    } else if (operationTransactionalAction instanceof org.mule.runtime.extension.api.tx.OperationTransactionalAction) {
      return fromLegacy((org.mule.runtime.extension.api.tx.OperationTransactionalAction) operationTransactionalAction);
    }
    throw new IllegalArgumentException(format("operationTransactionalAction is expected to be a org.mule.sdk.api.tx.OperationTransactionalAction or org.mule.runtime.extension.api.tx.OperationTransactionalAction, but was %s",
                                              operationTransactionalAction.getClass()));
  }

  /**
   * Returns the associated {@link OperationTransactionalAction} from the given value.
   *
   * @param operationTransactionalAction the value to take the {@link OperationTransactionalAction}
   * @return the {@link OperationTransactionalAction} associated to the given argument.
   */
  public static OperationTransactionalAction fromLegacy(org.mule.runtime.extension.api.tx.OperationTransactionalAction operationTransactionalAction) {
    switch (operationTransactionalAction) {
      case JOIN_IF_POSSIBLE:
        return OperationTransactionalAction.JOIN_IF_POSSIBLE;
      case NOT_SUPPORTED:
        return OperationTransactionalAction.NOT_SUPPORTED;
      case ALWAYS_JOIN:
        return OperationTransactionalAction.ALWAYS_JOIN;
    }
    return null;
  }

}
