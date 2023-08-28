/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
