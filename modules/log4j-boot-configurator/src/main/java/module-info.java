/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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

  // Third party modules
  requires com.lmax.disruptor;
  requires org.apache.logging.log4j.core;

  requires transitive org.apache.logging.log4j;
  requires transitive org.slf4j;
}
