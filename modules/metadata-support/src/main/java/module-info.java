/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Runtime support for Metadata Caches.
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.metadata.support {

  requires org.mule.runtime.metadata.model.api;
  requires org.mule.runtime.metadata.model.message;
  requires transitive org.mule.runtime.api;
  requires org.mule.runtime.extensions.api;
  requires org.mule.runtime.dsl.api;
  requires org.mule.runtime.artifact.ast;
  requires org.mule.runtime.core;
  requires org.mule.sdk.api;

  requires com.google.common;
  requires org.apache.commons.lang3;

  exports org.mule.runtime.metadata.api.dsl;
  exports org.mule.runtime.metadata.api.cache;
  exports org.mule.runtime.metadata.api.locator;

  exports org.mule.runtime.metadata.internal to
      org.mule.runtime.extensions.support,
      org.mule.runtime.spring.config,
      org.mule.runtime.tooling.support,
      spring.beans;
  exports org.mule.runtime.metadata.internal.cache to
      org.mule.runtime.spring.config,
      org.mule.runtime.tooling.support,
      spring.beans;

  opens org.mule.runtime.metadata.internal to
      spring.core;
  opens org.mule.runtime.metadata.internal.cache to
      spring.core;

}
