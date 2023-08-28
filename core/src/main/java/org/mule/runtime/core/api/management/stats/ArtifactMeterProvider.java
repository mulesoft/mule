/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
