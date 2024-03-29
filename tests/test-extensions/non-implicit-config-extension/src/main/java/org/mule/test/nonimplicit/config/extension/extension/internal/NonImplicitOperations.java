/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.nonimplicit.config.extension.extension.internal;

import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.test.nonimplicit.config.extension.extension.api.Counter;
import org.mule.test.nonimplicit.config.extension.extension.api.NonImplicitConfigExtension;

public class NonImplicitOperations {

  public NonImplicitConfigExtension getConfig(@org.mule.sdk.api.annotation.param.Config NonImplicitConfigExtension config) {
    return config;
  }

  public Counter getConnection(@Connection Counter connection) {
    return connection;
  }
}
