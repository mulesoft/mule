/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

import org.mule.runtime.environment.api.RuntimeEnvironment;
import org.mule.runtime.api.applicationserver.ApplicationServerRuntimeEnvironment;

/**
 * Mule Runtime Environment Application Server Module
 *
 * @moduleGraph
 * @since 4.7
 */
module org.mule.runtime.environment.applicationserver {

  requires org.mule.runtime.core;
  requires org.mule.runtime.deployment;
  requires org.mule.runtime.environment.api;

  opens org.mule.runtime.api.applicationserver;

  provides RuntimeEnvironment with
          ApplicationServerRuntimeEnvironment;
}