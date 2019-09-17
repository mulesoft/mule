/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
