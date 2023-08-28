/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.internal.metadata.DefaultDataTypeBuilder;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Threads;

@OutputTimeUnit(NANOSECONDS)
@Threads(3)
public class DataTypeBenchmark extends AbstractBenchmark {

  private DataType multiMapStringString = DataType.MULTI_MAP_STRING_STRING;

  @Benchmark
  public DataType multiMapDataType() {
    return new DefaultDataTypeBuilder(multiMapStringString).build();
  }

}
