/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter;

import static io.opentelemetry.api.common.AttributeKey.booleanKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.common.AttributeKey;

/**
 * Utils for exporting Open Telemetry Spans.
 *
 * @since 4.5.0
 */
public class OpenTelemetrySpanExporterUtils {

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
}
