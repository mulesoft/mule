/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.management.stats;

import org.mule.metrics.api.MeterProvider;
import org.mule.metrics.api.meter.builder.MeterBuilder;

import static org.mule.metrics.api.meter.MeterProperties.MULE_METER_ARTIFACT_ID_ATTRIBUTE;

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
    return meterProvider.getMeterBuilder(meterName).withMeterAttribute(MULE_METER_ARTIFACT_ID_ATTRIBUTE, artifactId);
  }

  @Override
  public void close() {
    // Nothing to do.
  }

  public String getArtifactId() {
    return artifactId;
  }
}
