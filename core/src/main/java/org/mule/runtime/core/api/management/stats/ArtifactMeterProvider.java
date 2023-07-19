/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.management.stats;

import org.mule.runtime.metrics.api.MeterProvider;
import org.mule.runtime.metrics.api.meter.builder.MeterBuilder;

/**
 * An {@link MeterProvider} associated to an artifact id.
 */
public class ArtifactMeterProvider implements MeterProvider {

  private final MeterProvider meterProvider;
  private final String artifactId;

  public ArtifactMeterProvider(MeterProvider meterProvider, String artifactId) {
    this.meterProvider = meterProvider;
    this.artifactId = artifactId;
  }

  @Override
  public MeterBuilder getMeterBuilder(String meterName) {
    return meterProvider.getMeterBuilder(meterName);
  }

  public String getArtifactId() {
    return artifactId;
  }
}
