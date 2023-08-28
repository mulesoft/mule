/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
/**
 * Mule Logging Module.
 * 
 * @moduleGraph
 * @since 4.5
 */
module org.mule.runtime.logging {

  // Allows usage of Unsafe for caffeine and disruptor libraries, used by the logging framework
  requires jdk.unsupported;

  // Logging framework
  requires transitive org.slf4j;
  requires transitive org.apache.logging.log4j;

  // Log bridges
  requires transitive jul.to.slf4j;
  requires transitive java.logging;
  requires transitive org.apache.commons.logging;

}
