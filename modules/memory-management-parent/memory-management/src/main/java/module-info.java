/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * This module performs memory management for the runtime.
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.memory.management {

  requires org.mule.runtime.api;
  requires org.mule.runtime.profiling.api;

  exports org.mule.runtime.internal.memory.management to
      org.mule.runtime.spring.config,
      org.mule.runtime.deployment.model.impl;
}
