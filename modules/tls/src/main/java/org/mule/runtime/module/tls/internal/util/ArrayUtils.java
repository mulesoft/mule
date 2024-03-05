/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tls.internal.util;

import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.Set;

public class ArrayUtils {

  /**
   * Calculates the intersection between two arrays, as if they were sets.
   *
   * @return A new array with the intersection.
   */
  public static String[] intersection(String[] a, String[] b) {
    Set<String> result = new HashSet<>();
    result.addAll(asList(a));
    result.retainAll(asList(b));
    return result.toArray(new String[result.size()]);
  }

}
