/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Mule service module. Provides services support to Mule container.
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.service {

  requires transitive org.mule.runtime.api;
  requires transitive org.mule.runtime.artifact;
  requires org.mule.runtime.container;
  requires org.mule.runtime.core;
  requires org.mule.runtime.jpms.utils;
  requires org.mule.runtime.metadata.model.java;

  requires com.google.common;
  requires org.apache.commons.io;
  requires org.apache.commons.lang3;
  requires org.reflections;

  exports org.mule.runtime.module.service.api.artifact;
  exports org.mule.runtime.module.service.api.discoverer;
  exports org.mule.runtime.module.service.api.manager;

  exports org.mule.runtime.module.service.internal.artifact to
      org.mule.test.runner;
  exports org.mule.runtime.module.service.internal.discoverer to
      org.mule.runtime.artifact.activation,
      org.mule.runtime.deployment.model.impl;
  exports org.mule.runtime.module.service.internal.manager to
      org.mule.runtime.spring.config,
      org.mule.runtime.deployment.model.impl;
  exports org.mule.runtime.module.service.internal.util to
      org.mule.runtime.spring.config;

}
