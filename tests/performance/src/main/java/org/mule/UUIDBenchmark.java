/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static org.mule.runtime.core.api.util.UUID.getClusterUUID;
import static org.mule.runtime.core.api.util.UUID.getUUID;

import org.mule.runtime.api.exception.MuleException;

import org.openjdk.jmh.annotations.Benchmark;

public class UUIDBenchmark extends AbstractBenchmark {

  @Benchmark
  public String UUID() throws MuleException {
    return getUUID();
  }

  @Benchmark
  public String clusterUUID() throws MuleException {
    return getClusterUUID(getClusterId());
  }

  @Benchmark
  public String clusterUUIDPrefix() throws MuleException {
    return getClusterUUID(getClusterIdPrefix());
  }

  private int getClusterId() {
    return 1;
  }

  private String getClusterIdPrefix() {
    return "1-";
  }

}
