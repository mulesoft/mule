/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * Bootstrap API for the Mule Container.
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.boot.api {

  exports org.mule.runtime.module.boot.api;
  exports org.mule.runtime.module.reboot.api;

  // This directs some exports to modules that are loaded in the container layer. Those are used for compile-time validation and
  // then directed programmatically by JpmsUtils
  exports org.mule.runtime.module.boot.internal to
      org.mule.boot,
      org.mule.boot.tanuki,
      org.mule.runtime.launcher, // container layer!
      com.mulesoft.mule.boot,
      com.mulesoft.mule.runtime.plugin; // container layer!

  // Needed by the BootModuleLayerValidationBootstrapConfigurer and for creating the container ClassLoader
  requires org.mule.runtime.jpms.utils;

  // Needed by the MuleLog4jConfigurer
  requires static org.mule.runtime.boot.log4j;

  // Needed by the SLF4JBridgeHandlerBootstrapConfigurer,
  requires jul.to.slf4j;
  // but also to make the logging modules available from the boot layer
  requires static org.mule.runtime.logging;

  requires org.apache.commons.cli;

  uses org.mule.runtime.module.boot.api.MuleContainerProvider;

  // Required to programmatically propagate accessibility by JpmsUtils
  opens org.mule.runtime.module.boot.internal to
      org.mule.runtime.jpms.utils;

}