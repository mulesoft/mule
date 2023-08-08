/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
/**
 * Mule Container Launcher module.
 * 
 * @moduleGraph
 * @since 4.5
 */
module org.mule.runtime.launcher {

  requires org.mule.boot;
  requires org.mule.runtime.api;
  requires org.mule.runtime.core;
  requires org.mule.runtime.repository;
  requires org.mule.runtime.artifact;
  requires org.mule.runtime.artifact.activation;
  requires org.mule.runtime.container;
  requires org.mule.runtime.service;
  requires org.mule.runtime.deployment;
  requires org.mule.runtime.deployment.model.impl;
  requires org.mule.runtime.log4j;
  requires org.mule.runtime.boot.log4j;
  requires org.mule.runtime.troubleshooting;
  requires org.mule.runtime.tooling.support;

  exports org.mule.runtime.module.launcher.coreextension to
      org.mule.runtime.core;

  provides org.mule.runtime.module.reboot.internal.MuleContainerProvider with
      org.mule.runtime.module.launcher.LauncherMuleContainerProvider;

}
