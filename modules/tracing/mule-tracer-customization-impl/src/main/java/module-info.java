/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Implementation for setting configuration for tracing levels and overrides.
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.tracer.customization.impl {

  requires org.mule.runtime.core;
  requires org.mule.runtime.tracer.api;
  requires org.mule.runtime.tracer.customization.api;
  requires org.mule.runtime.tracer.configuration.api;

  requires org.apache.commons.lang3;

  exports org.mule.runtime.tracer.customization.impl.provider to
      org.mule.runtime.spring.config,
      spring.beans;

}
