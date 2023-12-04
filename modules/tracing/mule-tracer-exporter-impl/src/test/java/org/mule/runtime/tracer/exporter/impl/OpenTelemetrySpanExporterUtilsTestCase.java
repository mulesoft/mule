/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.exporter.impl;

import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.getSampler;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.OPEN_TELEMETRY_EXPORTER;

import static io.opentelemetry.sdk.trace.samplers.Sampler.alwaysOff;
import static io.opentelemetry.sdk.trace.samplers.Sampler.alwaysOn;
import static io.opentelemetry.sdk.trace.samplers.Sampler.parentBased;
import static io.opentelemetry.sdk.trace.samplers.Sampler.parentBasedBuilder;
import static io.opentelemetry.sdk.trace.samplers.Sampler.traceIdRatioBased;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(OPEN_TELEMETRY_EXPORTER)
public class OpenTelemetrySpanExporterUtilsTestCase {

  @Test
  public void alwaysOnSampler() {
    assertThat(getSampler("always_on", null), equalTo(alwaysOn()));
  }

  @Test
  public void alwaysOffSampler() {
    assertThat(getSampler("always_off", null), equalTo(alwaysOff()));
  }

  @Test
  public void defaultSampler() {
    assertThat(getSampler(null, null), equalTo(alwaysOn()));
  }

  @Test
  public void traceIddRatioSampler() {
    Sampler sampler = getSampler("traceidratio", null);
    assertThat(sampler, equalTo(traceIdRatioBased(1.0d)));
  }

  @Test
  public void traceIddRatioSamplerWithRatioDifferentThanDefault() {
    Sampler sampler = getSampler("traceidratio", "0.5d");
    assertThat(sampler, equalTo(traceIdRatioBased(0.5d)));
  }

  @Test
  public void parentBasedIdAlwaysOn() {
    Sampler sampler = getSampler("parentbased_always_on", null);
    assertThat(sampler, equalTo(parentBased(alwaysOn())));
  }

  @Test
  public void parentBasedIdAlwaysOff() {
    Sampler sampler = getSampler("parentbased_always_off", null);
    assertThat(sampler, equalTo(parentBased(alwaysOff())));
  }

  @Test
  public void parentBasedTraceIdRatioWithRatioDifferentThanDefault() {
    Sampler sampler = getSampler("parentbased_traceidratio", "0.5d");
    assertThat(sampler, equalTo(parentBasedBuilder(traceIdRatioBased(0.5d)).build()));
  }

  @Test
  public void parentBasedTraceIdRatio() {
    Sampler sampler = getSampler("parentbased_traceidratio", null);
    assertThat(sampler, equalTo(parentBasedBuilder(traceIdRatioBased(1.0d)).build()));
  }

}
