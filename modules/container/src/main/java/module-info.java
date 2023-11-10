/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * Provides container artifact related functionality.
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.container {

  requires org.mule.runtime.api;
  requires org.mule.runtime.core;
  requires org.mule.runtime.jar.handling.utils;
  requires org.mule.runtime.artifact;
  requires org.mule.runtime.jpms.utils;

  requires org.apache.commons.io;
  requires org.apache.commons.lang3;
  requires com.google.common;

  exports org.mule.runtime.container.api;
  exports org.mule.runtime.container.api.discoverer;

  exports org.mule.runtime.container.internal to
      org.mule.runtime.artifact.activation,
      org.mule.runtime.deployment,
      org.mule.runtime.deployment.model,
      org.mule.runtime.deployment.model.impl,
      org.mule.runtime.service,
      org.mule.runtime.spring.config,
      org.mule.test.runner;
}
