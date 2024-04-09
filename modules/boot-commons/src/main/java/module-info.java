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
 *
 * Note: changes to this API will impact 'mule-embedded-api' and MUnit, since the version of this module is fixed independently of the Runtime version being used (see W-14853053 for reference).
 * Adding a method to this API and using it within the Runtime will cause newer versions of the Runtime to fail in instance of MUnit versions that use the old version of this module.
 */
module org.mule.boot.commons {

  exports org.mule.runtime.module.boot.commons.internal;

  // Needed by the BootModuleLayerValidationBootstrapConfigurer and for creating the container ClassLoader
  requires org.mule.runtime.jpms.utils;
  requires org.mule.boot.api;

  // Needed by the SLF4JBridgeHandlerBootstrapConfigurer, but also to make the logging modules available from the boot layer
  requires org.mule.runtime.logging;

  requires org.apache.commons.cli;
  requires org.slf4j;


  // Required to programmatically propagate accessibility by JpmsUtils
  opens org.mule.runtime.module.boot.commons to org.mule.runtime.jpms.utils,
          org.mule.runtime.module.boot.api;

}