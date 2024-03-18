/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.exporter.impl;

import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.ALWAYS_OFF_SAMPLER;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.ALWAYS_ON_SAMPLER;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.PARENTBASED_ALWAYS_OFF_SAMPLER;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.PARENTBASED_ALWAYS_ON_SAMPLER;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.PARENTBASED_TRACEIDRATIO_SAMPLER;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.TRACEIDRATIO_SAMPLER;
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(PROFILING)
@Story(OPEN_TELEMETRY_EXPORTER)
public class OpenTelemetrySpanExporterUtilsTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void alwaysOnSampler() {
    assertThat(getSampler(ALWAYS_ON_SAMPLER, ""), equalTo(alwaysOn()));
  }

  @Test
  public void alwaysOffSampler() {
    assertThat(getSampler(ALWAYS_OFF_SAMPLER, ""), equalTo(alwaysOff()));
  }

  @Test
  public void nullSampler() {
    expectedException.expectMessage("The sampler arg retrieved by configuration cannot be null");
    getSampler(null, null);
  }

  @Test
  public void invalidSampler() {
    expectedException.expectMessage("Sampler not valid. Sampler: zaraza Arg: arg");
    getSampler("zaraza", "arg");
  }

  @Test
  public void invalidRatioTraceId() {
    expectedException.expectMessage("The ratio is invalid: zaraza");
    getSampler(TRACEIDRATIO_SAMPLER, "zaraza");
  }

  @Test
  public void invalidRatioParentBased() {
    expectedException.expectMessage("The ratio is invalid: zaraza");
    getSampler(PARENTBASED_TRACEIDRATIO_SAMPLER, "zaraza");
  }

  @Test
  public void traceIdRatioSampler() {
    Sampler sampler = getSampler(TRACEIDRATIO_SAMPLER, "0.1");
    assertThat(sampler, equalTo(traceIdRatioBased(0.1d)));
  }

  @Test
  public void traceIddRatioSamplerWithRatioDifferentThanDefault() {
    Sampler sampler = getSampler(TRACEIDRATIO_SAMPLER, "0.5");
    assertThat(sampler, equalTo(traceIdRatioBased(0.5d)));
  }

  @Test
  public void parentBasedIdAlwaysOn() {
    Sampler sampler = getSampler(PARENTBASED_ALWAYS_ON_SAMPLER, "0.1");
    assertThat(sampler, equalTo(parentBased(alwaysOn())));
  }

  @Test
  public void parentBasedIdAlwaysOff() {
    Sampler sampler = getSampler(PARENTBASED_ALWAYS_OFF_SAMPLER, "0.1");
    assertThat(sampler, equalTo(parentBased(alwaysOff())));
  }

  @Test
  public void parentBasedTraceIdRatioWithRatioDifferentThanDefault() {
    Sampler sampler = getSampler(PARENTBASED_TRACEIDRATIO_SAMPLER, "0.5");
    assertThat(sampler, equalTo(parentBasedBuilder(traceIdRatioBased(0.5d)).build()));
  }

  @Test
  public void parentBasedTraceIdRatio() {
    Sampler sampler = getSampler(PARENTBASED_TRACEIDRATIO_SAMPLER, "0.1");
    assertThat(sampler, equalTo(parentBasedBuilder(traceIdRatioBased(0.1d)).build()));
  }

}
