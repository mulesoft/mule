/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * This module handles the global configuration of the Mule Runtime.
 *
 * @moduleGraph
 * @since 4.5
 */
module org.mule.runtime.global.config {

  requires org.mule.runtime.api;
  requires org.mule.runtime.api.annotations;
  requires org.mule.runtime.container;
  requires org.mule.runtime.core;
  requires transitive org.mule.runtime.maven.client.api;

  // com.typesafe:config dependency:
  requires config;
  requires org.everit.json.schema;
  requires org.json;

  exports org.mule.runtime.globalconfig.api;
  exports org.mule.runtime.globalconfig.api.cluster;
  exports org.mule.runtime.globalconfig.api.maven;

}
