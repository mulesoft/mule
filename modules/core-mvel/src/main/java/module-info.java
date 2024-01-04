/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.core.mvel {

  requires org.mule.runtime.core;
  requires org.apache.commons.collections4;
  requires org.mule.runtime.dsl.api;
  requires mule.mvel2;
  requires com.google.common;
  requires jakarta.activation;
  requires org.apache.commons.lang3;
  requires java.xml;

  exports org.mule.runtime.core.internal.el.mvel to
      org.mule.runtime.spring.config;

  exports org.mule.runtime.core.internal.el.mvel.configuration to
      org.mule.runtime.spring.config;
}
