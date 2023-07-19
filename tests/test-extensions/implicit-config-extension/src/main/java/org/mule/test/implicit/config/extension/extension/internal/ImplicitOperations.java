/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.implicit.config.extension.extension.internal;

import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.test.implicit.config.extension.extension.api.Counter;
import org.mule.test.implicit.config.extension.extension.api.ImplicitConfigExtension;

public class ImplicitOperations {

  public ImplicitConfigExtension getConfig(@org.mule.sdk.api.annotation.param.Config ImplicitConfigExtension config) {
    return config;
  }

  public Counter getConnection(@Connection Counter connection) {
    return connection;
  }
}
