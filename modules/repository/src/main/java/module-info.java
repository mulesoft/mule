/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * Mule repository module. Provides access to external dependencies required by the runtime.
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.repository {

  requires org.mule.runtime.api;
  requires org.mule.runtime.artifact;
  requires org.mule.runtime.core;
  requires org.mule.runtime.maven.client.api;
  requires org.slf4j;

  exports org.mule.runtime.module.repository.api;

}
