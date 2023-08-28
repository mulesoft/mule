/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.api.sniffer;

import java.util.Map;

/**
 * Encapsulates data corresponding to a captured exported span event. This is only used for testing purposes and is not exposed as
 * general API.
 *
 * @see ExportedSpanSniffer
 *
 * @since 4.5.0
 */
public interface CapturedEventData {

  /**
   * @return The name of the event.
   */
  String getName();

  /**
   * @return The attributes of the event.
   */
  Map<String, Object> getAttributes();

}
