/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * Mule service that allows to create HTTP servers and clients.
 *
 * @moduleGraph
 * @since 4.5
 */
module org.mule.runtime.integration.orchestrator {

  requires transitive org.mule.runtime.api;
  requires org.mule.runtime.core;
  requires org.mule.runtime.artifact;
  requires org.mule.runtime.artifact.ast;
  requires org.mule.runtime.artifact.ast.serialization;
  requires  org.mule.runtime.deployment.model;
  requires org.mule.runtime.deployment.model.impl;
  requires org.mule.runtime.deployment;
  requires org.mule.runtime.http.api;
  requires org.mule.sdk.api;

  requires semver4j;
  requires com.google.gson;

  exports org.mule.module.io.internal
    to org.mule.runtime.launcher;

  
}
