/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.exporter.impl;

import static org.mule.runtime.tracer.exporter.impl.MutableMuleTraceState.TRACE_STATE_KEY;

import static java.util.Collections.emptyMap;

import static io.opentelemetry.api.trace.propagation.internal.W3CTraceContextEncoding.encodeTraceState;

import org.mule.runtime.tracer.api.span.InternalSpan;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import io.opentelemetry.api.internal.OtelEncodingUtils;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;

/**
 * Utils for generating Open Telemetry Trace Ids
 *
 * @since 4.5.0
 */
public class OpenTelemetryTraceIdUtils {

  private OpenTelemetryTraceIdUtils() {}

  private static final Supplier<Random> randomSupplier = ThreadLocalRandom::current;

  private static final long INVALID_ID = 0;

  private static final ThreadLocal<char[]> CHAR_ARRAY = new ThreadLocal<>();

  private static final String INVALID = "0000000000000000";

  private static final String ALPHABET = "0123456789abcdef";

  private static final int BYTES_LENGTH = 16;

  private static final int HEX_LENGTH = 2 * BYTES_LENGTH;

  static final int BYTE_BASE16 = 2;

  private static final char[] ENCODING = buildEncodingArray();

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


  private static char[] buildEncodingArray() {
    char[] encoding = new char[512];
    for (int i = 0; i < 256; ++i) {
      encoding[i] = ALPHABET.charAt(i >>> 4);
      encoding[i | 0x100] = ALPHABET.charAt(i & 0xF);
    }
    return encoding;
  }

  public static String fromLong(long id) {
    if (id == 0) {
      return getInvalid();
    }
    char[] result = chars(HEX_LENGTH_SPAN_ID);
    longToBase16String(id, result, 0);
    return new String(result, 0, HEX_LENGTH_SPAN_ID);
  }

  public static void longToBase16String(long value, char[] dest, int destOffset) {
    byteToBase16((byte) (value >> 56 & 0xFFL), dest, destOffset);
    byteToBase16((byte) (value >> 48 & 0xFFL), dest, destOffset + BYTE_BASE16);
    byteToBase16((byte) (value >> 40 & 0xFFL), dest, destOffset + 2 * BYTE_BASE16);
    byteToBase16((byte) (value >> 32 & 0xFFL), dest, destOffset + 3 * BYTE_BASE16);
    byteToBase16((byte) (value >> 24 & 0xFFL), dest, destOffset + 4 * BYTE_BASE16);
    byteToBase16((byte) (value >> 16 & 0xFFL), dest, destOffset + 5 * BYTE_BASE16);
    byteToBase16((byte) (value >> 8 & 0xFFL), dest, destOffset + 6 * BYTE_BASE16);
    byteToBase16((byte) (value & 0xFFL), dest, destOffset + 7 * BYTE_BASE16);
  }

  public static void byteToBase16(byte value, char[] dest, int destOffset) {
    int b = value & 0xFF;
    dest[destOffset] = ENCODING[b];
    dest[destOffset + 1] = ENCODING[b | 0x100];
  }

  public static char[] chars(int len) {
    char[] buffer = CHAR_ARRAY.get();
    if (buffer == null || buffer.length < len) {
      buffer = new char[len];
      CHAR_ARRAY.set(buffer);
    }

    return buffer;
  }

  public static String fromLongs(long traceIdLongHighPart, long traceIdLongLowPart) {
    if (traceIdLongHighPart == 0 && traceIdLongLowPart == 0) {
      return getInvalid();
    }
    char[] chars = chars(HEX_LENGTH);
    longToBase16String(traceIdLongHighPart, chars, 0);
    longToBase16String(traceIdLongLowPart, chars, 16);
    return new String(chars, 0, HEX_LENGTH);
  }

  public static String getInvalid() {
    return INVALID;
  }

  public static String generateSpanId() {
    long id;
    Random random = randomSupplier.get();
    do {
      id = random.nextLong();
    } while (id == INVALID_ID);
    return fromLong(id);
  }

  public static String generateTraceId(InternalSpan parentSpan) {
    if (parentSpan != null && parentSpan.getIdentifier().isValid()) {
      return parentSpan.getIdentifier().getTraceId();
    } else {
      Random random = randomSupplier.get();
      long idHi = random.nextLong();
      long idLo;
      do {
        idLo = random.nextLong();
      } while (idLo == INVALID_ID);
      return fromLongs(idHi, idLo);
    }
  }

  /**
   * Gets a map to propagate a trace context for open telemetry.
   *
   * @param openTelemetrySpanExporter the {@link OpenTelemetrySpanExporter}
   * @param isAddMuleAncestorSpanId   whether it has to add the mule ancestor.
   *
   * @return the map with that represents the distributed trace context.
   */
  public static Map<String, String> getDistributedTraceContext(OpenTelemetrySpanExporter openTelemetrySpanExporter,
                                                               boolean isAddMuleAncestorSpanId) {
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
    if (isAddMuleAncestorSpanId) {
      context.put(TRACE_STATE_KEY,
                  encodeTraceState(openTelemetrySpanExporter.getTraceState()
                      .withAncestor(openTelemetrySpanExporter.getSpanId())));
    } else {
      context.put(TRACE_STATE_KEY,
                  encodeTraceState(openTelemetrySpanExporter.getTraceState()));
    }

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

  private static final Set<String> VALID_VERSIONS = new HashSet();

  static {
    for (int i = 0; i < 255; ++i) {
      String version = Long.toHexString((long) i);
      if (version.length() < 2) {
        version = '0' + version;
      }

      VALID_VERSIONS.add(version);
    }

  }
}
