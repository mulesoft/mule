/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
