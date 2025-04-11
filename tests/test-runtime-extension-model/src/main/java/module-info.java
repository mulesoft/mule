/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

import org.mule.runtime.extension.api.provider.RuntimeExtensionModelProvider;

/**
 * @moduleGraph
 * @since 4.5
 */
module org.mule.test.runtime.extension.model {

  requires org.mule.runtime.api;
  requires org.mule.runtime.extensions.api;
  requires org.mule.runtime.extension.model;

  requires org.mockito;

  provides RuntimeExtensionModelProvider with
      org.mule.runtime.module.artifact.activation.internal.extension.discovery.test.TestRuntimeExtensionModelProvider;
}
