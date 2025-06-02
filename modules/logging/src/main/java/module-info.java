/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * Mule Logging Module.
 *
 * @moduleGraph
 * @since 4.5
 */
module org.mule.runtime.logging {

  exports org.mule.runtime.module.log4j.api;

  // Allows usage of Unsafe for caffeine, required for DataWeave
  // TODO: remove after TD-0231495 is done
  requires jdk.unsupported;

  // Logging framework
  requires org.slf4j;
  requires org.apache.logging.log4j;

  // These logging implementations have to be exported so any artifact code using those libs is hooked to Mule's logging
  // mechanism.
  // Otherwise, code from an application lib using, for instance, commons-logging, will use its own commons-logging instead of the
  // slf4j bridge from the container.
  requires transitive org.apache.logging.log4j.core;

  // Log bridges
  requires transitive jul.to.slf4j;
  requires transitive java.logging;
  requires transitive org.apache.commons.logging;
  requires transitive org.apache.log4j;

}
