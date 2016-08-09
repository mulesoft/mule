/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.infrastructure;

import org.mule.runtime.config.spring.dsl.api.ObjectFactory;
import org.mule.runtime.module.extension.internal.config.DefaultExtensionConfig;
import org.mule.runtime.module.extension.internal.config.ExtensionConfig;

/**
 * An {@link ObjectFactory} which produces {@link ExtensionConfig} instances
 *
 * @since 4.0
 */
public class ExtensionConfigObjectFactory implements ObjectFactory<ExtensionConfig> {

  private DynamicConfigurationExpiration dynamicConfigurationExpiration;

  @Override
  public ExtensionConfig getObject() throws Exception {
    DefaultExtensionConfig config = new DefaultExtensionConfig();
    if (dynamicConfigurationExpiration != null) {
      config.setDynamicConfigExpirationFrequency(dynamicConfigurationExpiration.getFrequency());
    }

    return config;
  }

  public void setDynamicConfigurationExpiration(DynamicConfigurationExpiration dynamicConfigurationExpiration) {
    this.dynamicConfigurationExpiration = dynamicConfigurationExpiration;
  }
}
