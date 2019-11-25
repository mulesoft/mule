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

  public static final String IRON_MAN = "IronMan";
  public static final String CAPTAIN_AMERICA = "Captain America";
  public static final String BLACK_WIDOW = "Black Widow";
  public static final String ANT_MAN = "Ant Man";
  public static final String ANT_MAN1 = "Ant Man";
  public static final String DR_STRANGE = "Dr. Strange";

  @TearDown
  public void teardown() throws MuleException {}

  @Benchmark
  public Object hashMapOf3() {
    Map<String, String> map = new HashMap<>();
    populate3(map);

    map.get(IRON_MAN);
    map.get(DR_STRANGE);

    return map;
  }

  @Benchmark
  public Object smallMapOf3() {
    Map<String, String> map = new SmallMap<>();
    populate3(map);

    map.get(IRON_MAN);
    map.get(DR_STRANGE);
    return map;
  }

  //@Benchmark
  public Object fullHashMap() {
    Map<String, String> map = new HashMap<>();
    populateFive(map);
    return map;
  }

  //@Benchmark
  public Object fullSmallMap() {
    Map<String, String> map = new SmallMap<>();
    populateFive(map);
    return map;
  }

  private void populateFive(Map<String, String> map) {
    map.put(IRON_MAN, "Mr. Stark");
    map.put(CAPTAIN_AMERICA, "Steve Rodgers");
    map.put(BLACK_WIDOW, "Natasha Rommanoff");
    map.put(ANT_MAN, "Hank Pim");
    map.put(ANT_MAN1, "Hank Pim");
    map.put(DR_STRANGE, "Dr. Strange");

    map.get("IronMan");
    map.get("Ant Man");

    //Reference<String> r = new Reference<>();
    //map.entrySet().forEach(entry -> r.set(entry.getKey() + entry.getValue()));
  }

  private void populate3(Map<String, String> map) {
    map.put(IRON_MAN, "Mr. Stark");
    map.put(CAPTAIN_AMERICA, "Steve Rodgers");
    map.put(BLACK_WIDOW, "Natasha Rommanoff");
    map.put(BLACK_WIDOW, "Natasha Rommanoff");

    //Reference<String> r = new Reference<>();
    //map.entrySet().forEach(entry -> r.set(entry.getKey() + entry.getValue()));
  }

}
