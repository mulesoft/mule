/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.util;

import java.util.Collection;

public final class WildcardAttributeEvaluator {

  private final String escapedValue;
  private final Boolean hasWildcards;

  public WildcardAttributeEvaluator(String attributeValue) {
    if (attributeValue == null) {
      throw new IllegalArgumentException("null not allowed");
    }
    this.escapedValue = attributeValue.replaceAll("\\*", "*");
    hasWildcards = attributeValue.startsWith("*") || (attributeValue.endsWith("*") && !attributeValue.endsWith("\\*"))
        || attributeValue.equals("*");
  }

  public boolean hasWildcards() {
    return hasWildcards;
  }

  public void processValues(Collection<String> values, MatchCallback matchCallback) {
    if (!hasWildcards()) {
      throw new IllegalStateException("Can't call processValues with non wildcard attribute");
    }
    for (String value : values) {
      if (matches(value)) {
        matchCallback.processMatch(value);
      }
    }
  }

  public boolean matches(String value) {
    if (value == null) {
      return false;
    }
    if (escapedValue.equals("*")) {
      return true;
    } else if (escapedValue.startsWith("*")) {
      return value.endsWith(escapedValue.substring(1, escapedValue.length()));
    } else if (escapedValue.endsWith("*")) {
      return value.startsWith(escapedValue.substring(0, escapedValue.length() - 1));
    }
    return false;
  }

  public interface MatchCallback {

    public void processMatch(String matchedValue);
  }
}
