/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.source.legacy;

import org.mule.sdk.api.runtime.source.BackPressureAction;

/**
 * Utils class for handling {@link BackPressureAction}
 *
 * @since 4.5.0
 */
public final class SdkBackPressureActionUtils {

  private SdkBackPressureActionUtils() {}

  /**
   * Transforms a {@link org.mule.runtime.extension.api.runtime.source.BackPressureAction} into a {@link BackPressureAction}
   *
   * @param backPressureAction a {@link org.mule.runtime.extension.api.runtime.source.BackPressureAction}
   * @return the corresponding {@link BackPressureAction} to the given argument.
   */
  public static final BackPressureAction from(org.mule.runtime.extension.api.runtime.source.BackPressureAction backPressureAction) {
    switch (backPressureAction) {
      case FAIL:
        return BackPressureAction.FAIL;
      case DROP:
        return BackPressureAction.DROP;
    }
    return null;
  }

}
