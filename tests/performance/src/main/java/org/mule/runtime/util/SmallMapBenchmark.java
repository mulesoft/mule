/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.util;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import org.mule.AbstractBenchmark;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.util.collection.SmallMap;

import java.util.HashMap;
import java.util.Map;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.TearDown;

@OutputTimeUnit(NANOSECONDS)
public class SmallMapBenchmark extends AbstractBenchmark {

  private static final String[] KEYS = new String[] {"one", "two", "three", "four", "five", "six", "seven"};
  private static final String[] VALUES = new String[] {"uno", "dos", "tres", "cuatro", "cinco", "seis", "siete"};

  @TearDown
  public void teardown() throws MuleException {}

  @Benchmark
  public Object hashMapOf3() {
    Map<String, String> map = new HashMap<>();
    populate(map, 3);

    map.get("one");
    map.get("three");

    return map;
  }

  @Benchmark
  public Object smallMapOf3() {
    Map<String, String> map = new SmallMap<>();
    populate(map, 3);

    map.get("one");
    map.get("three");
    return map;
  }

  @Benchmark
  public Object hashMapOf6() {
    Map<String, String> map = new HashMap<>();
    populate(map, 6);

    map.get("one");
    map.get("three");
    map.get("six");

    return map;
  }

  @Benchmark
  public Object smallMapOf6() {
    Map<String, String> map = new SmallMap<>();
    populate(map, 6);

    map.get("one");
    map.get("three");
    map.get("six");

    return map;
  }

  private void populate(Map<String, String> map, int size) {
    for (int i = 0; i < size; i++) {
      map.put(KEYS[i], VALUES[i]);
    }
  }
}
