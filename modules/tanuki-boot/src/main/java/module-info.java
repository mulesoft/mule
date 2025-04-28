/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * Container Wrapper for Tanuki integration
 *
 * @moduleGraph
 * @since 4.5
 */
module org.mule.boot.tanuki {

  exports org.mule.runtime.module.boot.tanuki.internal to org.mule.boot.api;

  requires org.mule.boot.api;

  // Tanuki wrapper
  requires wrapper;
  requires org.apache.commons.cli;

}