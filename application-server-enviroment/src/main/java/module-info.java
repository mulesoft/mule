/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Mule Deployment Model Implementation Module.
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.environment.applicationserver {

  requires org.mule.runtime.core;
  requires org.mule.runtime.deployment;
  requires org.mule.runtime.environment;

  opens org.mule.runtime.environment.applicationserver;

  provides  org.mule.runtime.environment.RuntimeEnvironment with
          org.mule.runtime.environment.applicationserver.ApplicationServerRuntimeEnvironment;
}