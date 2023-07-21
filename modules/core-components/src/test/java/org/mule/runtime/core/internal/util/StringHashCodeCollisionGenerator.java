/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
