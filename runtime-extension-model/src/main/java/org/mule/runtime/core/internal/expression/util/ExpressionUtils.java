/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.expression.util;

public final class ExpressionUtils {

  private static final String DEFAULT_EXPRESSION_PREFIX = "#[";
  private static final String DEFAULT_EXPRESSION_SUFFIX = "]";

  private ExpressionUtils() {
    // Empty constructor in order to prevent this class from being accidentally instantiated.
  }

  /**
   * Checks if certain string value is an expression.
   *
   * @param value the possible expression.
   * @return {@code true} if the given value is an expression, or {@code false} otherwise.
   */
  public static boolean isExpression(String value) {
    if (value == null) {
      return false;
    }

    String trimmed = value.trim();
    return trimmed.startsWith(DEFAULT_EXPRESSION_PREFIX) && trimmed.endsWith(DEFAULT_EXPRESSION_SUFFIX);
  }

  public static String getUnfixedExpression(String value) {
    if (!isExpression(value)) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.substring(2, trimmed.length() - 1);
  }
}
