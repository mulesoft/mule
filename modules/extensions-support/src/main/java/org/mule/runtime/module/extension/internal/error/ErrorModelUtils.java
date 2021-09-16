/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.error;

import org.mule.sdk.api.error.ErrorTypeDefinition;
import org.mule.sdk.api.error.MuleErrors;

public final class ErrorModelUtils {

  public static boolean isMuleError(ErrorTypeDefinition errorType) {
    if (errorType instanceof LegacyErrorTypeDefinitionAdapter) {
      return ((LegacyErrorTypeDefinitionAdapter<?>) errorType)
          .getDelegate() instanceof org.mule.runtime.extension.api.error.MuleErrors;
    }

    return errorType instanceof MuleErrors;
  }

  private ErrorModelUtils() {}
}
