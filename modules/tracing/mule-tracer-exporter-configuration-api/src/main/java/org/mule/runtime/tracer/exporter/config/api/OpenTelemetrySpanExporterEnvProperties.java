package org.mule.runtime.tracer.exporter.config.api;

/**
 * Env properties for Span Configuration Exporter.
 *
 * @since 4.5.0
 */
public class OpenTelemetrySpanExporterEnvProperties {

    /**
     * Environemental property for the sampler as set by the SDK.
     *
     * @since 4.7.0
     */
    public static final String OTEL_TRACES_SAMPLER_ENV = "OTEL_TRACES_SAMPLER";

    /**
     * Environemental property for the sampler arg as set by the SDK.
     *
     * @since 4.7.0
     */
    public static final String OTEL_TRACES_SAMPLER_ARG_ENV = "OTEL_TRACES_SAMPLER_ARG";
}
