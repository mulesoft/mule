/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
