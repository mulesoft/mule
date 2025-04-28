/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

import org.mule.runtime.module.launcher.privileged.ContainerServiceProvider;

/**
 * Mule Container Launcher module.
 * 
 * @moduleGraph
 * @since 4.5
 */
module org.mule.runtime.launcher {

  requires org.mule.boot.api;
  requires org.mule.runtime.api;
  requires org.mule.runtime.artifact;
  requires org.mule.runtime.artifact.activation;
  requires org.mule.runtime.boot.log4j;
  requires org.mule.runtime.container;
  requires org.mule.runtime.core;
  requires org.mule.runtime.deployment;
  requires org.mule.runtime.deployment.model.impl;
  requires org.mule.runtime.log4j;
  requires org.mule.runtime.manifest;
  requires org.mule.runtime.repository;
  requires org.mule.runtime.service;
  requires org.mule.runtime.tls;

  requires org.apache.commons.io;
  requires org.apache.commons.lang3;

  // Embedded uses this
  exports org.mule.runtime.module.launcher;

  exports org.mule.runtime.module.launcher.privileged to
      org.mule.runtime.tooling.support,
      org.mule.runtime.troubleshooting;

  exports org.mule.runtime.module.launcher.coreextension to
      org.mule.runtime.core,
      com.mulesoft.mule.runtime.plugin;

  uses ContainerServiceProvider;

  provides org.mule.runtime.module.boot.api.MuleContainerProvider with
      org.mule.runtime.module.launcher.LauncherMuleContainerProvider;

}
