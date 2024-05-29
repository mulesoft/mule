/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Mule server and core utilities.
 *
 * @moduleGraph
 * @since 4.8
 */
module org.mule.runtime.core.utils {

  requires org.mule.runtime.api;

  requires com.google.common;
  requires org.apache.commons.collections4;
  requires org.apache.commons.io;
  requires org.apache.commons.lang3;

  exports org.mule.runtime.core.util.api;

  exports org.mule.runtime.core.util.internal to
      org.mule.runtime.core,
      org.mule.runtime.container,
      org.mule.runtime.deployment.model.impl;
}
