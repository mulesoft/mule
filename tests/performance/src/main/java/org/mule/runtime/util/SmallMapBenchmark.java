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
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.api.util.collection.FastMap;

import java.util.HashMap;
import java.util.Map;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.TearDown;

@OutputTimeUnit(NANOSECONDS)
public class SmallMapBenchmark extends AbstractBenchmark {

  @TearDown
  public void teardown() throws MuleException {}

  @Benchmark
  public Object halfHashMap() {
    Map<String, String> map = new HashMap<>();
    testHalfMap(map);
    return map;
  }

  @Benchmark
  public Object halfSmallMap() {
    Map<String, String> map = new FastMap<>();
    testHalfMap(map);
    return map;
  }

  @Benchmark
  public Object fullHashMap() {
    Map<String, String> map = new HashMap<>();
    testFullMap(map);
    return map;
  }

  @Benchmark
  public Object fullSmallMap() {
    Map<String, String> map = new FastMap<>();
    testFullMap(map);
    return map;
  }

  private void testFullMap(Map<String, String> map) {
    map.put("IronMan", "Mr. Stark");
    map.put("Captain America", "Steve Rodgers");
    map.put("Black Widow", "Natasha Rommanoff");
    map.put("Ant Man", "Hank Pim");
    map.put("Ant Man", "Hank Pim");
    map.put("Dr. Strange", "Dr. Strange");


    Reference<String> r = new Reference<>();
    map.entrySet().forEach(entry -> r.set(entry.getKey() + entry.getValue()));
  }

  private void testHalfMap(Map<String, String> map) {
    map.put("IronMan", "Mr. Stark");
    map.put("Captain America", "Steve Rodgers");
    map.put("Black Widow", "Natasha Rommanoff");
    map.put("Black Widow", "Natasha Rommanoff");

    Reference<String> r = new Reference<>();
    map.entrySet().forEach(entry -> r.set(entry.getKey() + entry.getValue()));
  }

}
