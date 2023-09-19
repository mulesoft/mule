/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * Module containing an injectable interface capable to call certain troubleshooting operations.
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.troubleshooting {

  requires org.mule.runtime.api.annotations;
  requires org.mule.runtime.core;
  requires org.mule.runtime.deployment;
  requires org.mule.runtime.deployment.model;

  requires org.json;
  requires com.google.gson;

  exports org.mule.runtime.module.troubleshooting.api;
}