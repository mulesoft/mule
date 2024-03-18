/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

module org.mule.boot.commons {

  exports org.mule.runtime.module.boot.commons.internal to
          org.mule.boot,
          org.mule.boot.tanuki,
          org.mule.runtime.launcher, // container layer!
          com.mulesoft.mule.boot,
          com.mulesoft.mule.runtime.plugin; // container layer!


  // Needed by the BootModuleLayerValidationBootstrapConfigurer and for creating the container ClassLoader
  requires org.mule.runtime.jpms.utils;
  requires org.mule.boot.api;

  // Needed by the SLF4JBridgeHandlerBootstrapConfigurer, but also to make the logging modules available from the boot layer
  requires org.mule.runtime.logging;

  requires org.apache.commons.cli;
  requires org.slf4j;
  requires org.mule.runtime.boot.log4j;


  // Required to programmatically propagate accessibility by JpmsUtils
  opens org.mule.runtime.module.boot.commons.internal to org.mule.runtime.jpms.utils;

}