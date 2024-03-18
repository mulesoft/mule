/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.exporter.impl;

import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.PARENTBASED_ALWAYS_ON_SAMPLER;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.ALWAYS_ON_SAMPLER;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.ALWAYS_OFF_SAMPLER;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.TRACEIDRATIO_SAMPLER;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.PARENTBASED_ALWAYS_OFF_SAMPLER;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.PARENTBASED_TRACEIDRATIO_SAMPLER;

import static java.lang.Double.parseDouble;
import static java.lang.String.format;

import static io.opentelemetry.api.common.AttributeKey.booleanKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.sdk.trace.samplers.Sampler.alwaysOff;
import static io.opentelemetry.sdk.trace.samplers.Sampler.alwaysOn;
import static io.opentelemetry.sdk.trace.samplers.Sampler.parentBased;
import static io.opentelemetry.sdk.trace.samplers.Sampler.traceIdRatioBased;
import static org.slf4j.LoggerFactory.getLogger;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.trace.samplers.Sampler;

import org.slf4j.Logger;

/**
 * Utils for exporting Open Telemetry Spans.
 *
 * @since 4.5.0
 */
public class OpenTelemetrySpanExporterUtils {

  private static final Logger LOGGER = getLogger(OpenTelemetrySpanExporterUtils.class);

  private OpenTelemetrySpanExporterUtils() {}

  public static final String EXCEPTION_EVENT_NAME = "exception";
  public static final AttributeKey<String> EXCEPTION_TYPE_KEY = stringKey("exception.type");
  public static final AttributeKey<String> EXCEPTION_MESSAGE_KEY = stringKey("exception.message");
  public static final AttributeKey<String> EXCEPTION_STACK_TRACE_KEY = stringKey("exception.stacktrace");
  public static final AttributeKey<Boolean> EXCEPTION_ESCAPED_KEY = booleanKey("exception.escaped");
  public static final AttributeKey<String> THREAD_END_NAME_KEY = stringKey("thread.end.name");
  public static final String EXCEPTIONS_HAVE_BEEN_RECORDED = "Exceptions have been recorded.";

  public static final AttributeKey<String> ARTIFACT_ID = stringKey("artifact.id");
  public static final AttributeKey<String> ARTIFACT_TYPE = stringKey("artifact.type");

  public static final String SPAN_KIND = "span.kind.override";
  public static final String STATUS = "status.override";

  public static String getNameWithoutNamespace(String name) {
    int index = name.lastIndexOf(":");
    if (index != -1) {
      return name.substring(index + 1);
    } else {
      return name;
    }
  }

  public static Sampler getSampler(String sampler, String samplerArg) {
    if (sampler == null) {
      throw new IllegalArgumentException("The sampler arg retrieved by configuration cannot be null.");
    }

    switch (sampler) {
      case ALWAYS_ON_SAMPLER:
        return alwaysOn();
      case ALWAYS_OFF_SAMPLER:
        return alwaysOff();
      case TRACEIDRATIO_SAMPLER: {
        return traceIdRatioBased(resolveRatio(samplerArg));
      }
      case PARENTBASED_ALWAYS_ON_SAMPLER:
        return parentBased(alwaysOn());
      case PARENTBASED_ALWAYS_OFF_SAMPLER:
        return parentBased(alwaysOff());
      case PARENTBASED_TRACEIDRATIO_SAMPLER: {
        return parentBased(traceIdRatioBased(resolveRatio(samplerArg)));
      }
      default:
        throw new IllegalArgumentException(format("Sampler not valid. Sampler: %s Arg: %s", sampler, samplerArg));
    }
  }

  private static double resolveRatio(String samplerArg) {
    try {
      return parseDouble(samplerArg);
    } catch (Exception e) {
      throw new IllegalArgumentException(format("The ratio is invalid: %s", samplerArg));
    }
  }
}
