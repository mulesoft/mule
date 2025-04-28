/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * Definitions for Mule Metrics.
 *
 * @moduleGraph
 * @since 4.5
 */
module org.mule.runtime.metrics.api {

  requires org.mule.runtime.api;

  exports org.mule.runtime.metrics.api;
  exports org.mule.runtime.metrics.api.instrument;
  exports org.mule.runtime.metrics.api.instrument.builder;
  exports org.mule.runtime.metrics.api.meter;
  exports org.mule.runtime.metrics.api.meter.builder;
  exports org.mule.runtime.metrics.api.error;

}
