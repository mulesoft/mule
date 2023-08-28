/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
