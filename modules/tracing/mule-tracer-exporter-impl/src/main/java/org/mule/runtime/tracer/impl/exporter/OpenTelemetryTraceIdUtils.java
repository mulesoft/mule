/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter;

import static io.opentelemetry.sdk.trace.IdGenerator.random;

import static java.util.Collections.emptyMap;

import io.opentelemetry.api.internal.OtelEncodingUtils;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Utils for generating Open Telemetry Trace Ids
 *
 * @since 4.5.0
 */
public class OpenTelemetryTraceIdUtils {

  private OpenTelemetryTraceIdUtils() {}

  private static final ThreadLocal<char[]> CHAR_ARRAY = new ThreadLocal<>();

  private static final String INVALID = "0000000000000000";

  private static final int BYTES_LENGTH = 16;

  private static final int HEX_LENGTH = 2 * BYTES_LENGTH;

  static final String TRACE_PARENT = "traceparent";

  private static final int HEX_LENGTH_SPAN_ID = 16;
  private static final String VERSION = "00";
  private static final int VERSION_SIZE = 2;
  private static final char TRACEPARENT_DELIMITER = '-';
  private static final int TRACEPARENT_DELIMITER_SIZE = 1;
  private static final int TRACE_ID_HEX_SIZE = HEX_LENGTH;
  private static final int SPAN_ID_HEX_SIZE = HEX_LENGTH_SPAN_ID;
  private static final int TRACE_OPTION_HEX_SIZE = 2;
  private static final int TRACE_ID_OFFSET = VERSION_SIZE + TRACEPARENT_DELIMITER_SIZE;
  private static final int SPAN_ID_OFFSET =
      TRACE_ID_OFFSET + TRACE_ID_HEX_SIZE + TRACEPARENT_DELIMITER_SIZE;
  private static final int TRACE_OPTION_OFFSET =
      SPAN_ID_OFFSET + SPAN_ID_HEX_SIZE + TRACEPARENT_DELIMITER_SIZE;
  private static final int TRACEPARENT_HEADER_SIZE = TRACE_OPTION_OFFSET + TRACE_OPTION_HEX_SIZE;

  public static char[] chars(int len) {
    char[] buffer = CHAR_ARRAY.get();
    if (buffer == null || buffer.length < len) {
      buffer = new char[len];
      CHAR_ARRAY.set(buffer);
    }

    return buffer;
  }

  public static String getInvalid() {
    return INVALID;
  }

  public static String generateSpanId() {
    return random().generateSpanId();
  }

  public static String generateTraceId() {
    return random().generateTraceId();
  }

  public static Map<String, String> getContext(OpenTelemetrySpanExporter openTelemetrySpanExporter) {
    Map<String, String> context = new HashMap<>();
    if (openTelemetrySpanExporter.getSpanId().equals(INVALID)) {
      return emptyMap();
    }

    char[] chars = chars(TRACEPARENT_HEADER_SIZE);
    chars[0] = VERSION.charAt(0);
    chars[1] = VERSION.charAt(1);
    chars[2] = TRACEPARENT_DELIMITER;

    String traceId = openTelemetrySpanExporter.getTraceId();
    traceId.getChars(0, traceId.length(), chars, TRACE_ID_OFFSET);

    chars[SPAN_ID_OFFSET - 1] = TRACEPARENT_DELIMITER;

    String spanId = openTelemetrySpanExporter.getSpanId();
    spanId.getChars(0, spanId.length(), chars, SPAN_ID_OFFSET);

    chars[TRACE_OPTION_OFFSET - 1] = TRACEPARENT_DELIMITER;
    chars[TRACE_OPTION_OFFSET] = '0';
    chars[TRACE_OPTION_OFFSET + 1] = '1';
    context.put(TRACE_PARENT, new String(chars, 0, TRACEPARENT_HEADER_SIZE));

    return context;
  }

  public static SpanContext extractContextFromTraceParent(String traceparent) {
    boolean isValid = traceparent != null && (traceparent.length() == TRACEPARENT_HEADER_SIZE
        || traceparent.length() > TRACEPARENT_HEADER_SIZE && traceparent.charAt(TRACEPARENT_HEADER_SIZE) == '-')
        && traceparent.charAt(2) == '-' && traceparent.charAt(SPAN_ID_OFFSET - 1) == '-'
        && traceparent.charAt(TRACE_OPTION_OFFSET - 1) == '-';
    if (!isValid) {
      return SpanContext.getInvalid();
    } else {
      String version = traceparent.substring(0, 2);
      if (!VALID_VERSIONS.contains(version)) {
        return SpanContext.getInvalid();
      } else if (version.equals("00") && traceparent.length() > TRACEPARENT_HEADER_SIZE) {
        return SpanContext.getInvalid();
      } else {
        String traceId = traceparent.substring(3, 3 + TraceId.getLength());
        String spanId = traceparent.substring(SPAN_ID_OFFSET, SPAN_ID_OFFSET + SpanId.getLength());
        char firstTraceFlagsChar = traceparent.charAt(TRACE_OPTION_OFFSET);
        char secondTraceFlagsChar = traceparent.charAt(TRACE_OPTION_OFFSET + 1);
        if (OtelEncodingUtils.isValidBase16Character(firstTraceFlagsChar)
            && OtelEncodingUtils.isValidBase16Character(secondTraceFlagsChar)) {
          TraceFlags traceFlags =
              TraceFlags.fromByte(OtelEncodingUtils.byteFromBase16(firstTraceFlagsChar, secondTraceFlagsChar));
          return SpanContext.createFromRemoteParent(traceId, spanId, traceFlags, TraceState.getDefault());
        } else {
          return SpanContext.getInvalid();
        }
      }
    }
  }

  private static final HashSet VALID_VERSIONS = new HashSet();

  static {
    for (int i = 0; i < 255; ++i) {
      String version = Long.toHexString(i);
      if (version.length() < 2) {
        version = '0' + version;
      }

      VALID_VERSIONS.add(version);
    }

  }
}
