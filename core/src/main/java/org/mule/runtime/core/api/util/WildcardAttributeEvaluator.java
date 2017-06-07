/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import java.util.Collection;

public class WildcardAttributeEvaluator {

  private String escapedValue;
  private Boolean hasWildcards;

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
    String[] valuesArray = values.toArray(new String[values.size()]);
    for (String value : valuesArray) {
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
