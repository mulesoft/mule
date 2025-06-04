/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Mule Log4j Configurator Module.
 *
 * @moduleGraph
 * @since 4.5
 */
module org.mule.runtime.boot.log4j {

  // Exported.
  exports org.mule.runtime.module.log4j.boot.api;

  requires org.mule.runtime.logging;

  // Third party modules
  requires com.lmax.disruptor;
  requires org.apache.logging.log4j.core;

  requires transitive org.apache.logging.log4j;
  requires org.slf4j;
}
