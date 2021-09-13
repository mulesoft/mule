/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

public class StringHashCodeCollisionGenerator {

  public static List<String> stringsWithSameHashCode(int desiredRecords) {
    List<String> strings = asList("Aa", "BB");
    List<String> temp = new ArrayList<>();

    boolean finished = false;
    for (int i = 0; i < 5 && !finished; i++) {
      temp = new ArrayList<>();
      int count = 0;
      for (String s : strings) {
        for (String t : strings) {
          if (count == desiredRecords) {
            finished = true;
            break;
          }
          temp.add(s + t);
          count++;
        }
      }
      strings = temp;
    }
    strings = temp;

    return strings;
  }
}
