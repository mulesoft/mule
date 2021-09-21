/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.error;

import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.sdk.api.error.ErrorTypeDefinition;
import org.mule.sdk.api.error.MuleErrors;

/**
 * {@link ErrorModel} handling utilities
 */
public final class ErrorModelUtils {

  /**
   * @param errorType an {@link ErrorTypeDefinition}
   * @return whether the given {@code errorType} represents a core mule language error ({@code true}) or a custom one
   *         ({@code false})
   */
  public static boolean isMuleError(ErrorTypeDefinition errorType) {
    if (errorType instanceof SdkErrorTypeDefinitionAdapter) {
      return ((SdkErrorTypeDefinitionAdapter<?>) errorType)
          .getDelegate() instanceof org.mule.runtime.extension.api.error.MuleErrors;
    }

    return errorType instanceof MuleErrors;
  }

  private ErrorModelUtils() {}
}
