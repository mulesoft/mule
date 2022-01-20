/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONFIGURATION_PROPERTIES;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.core.internal.el.function.MuleFunctionsBindingContextProvider;
import org.mule.runtime.core.privileged.el.GlobalBindingContextProvider;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.beans.factory.FactoryBean;

/**
 * 
 * @since 4.5
 */
public class MuleFunctionsBindingContextProviderRefreshPropertiesFactoryBean
    implements FactoryBean<GlobalBindingContextProvider> {

  @Inject
  private MuleFunctionsBindingContextProvider muleFunctionsBindingContextProvider;

  @Inject
  @Named(OBJECT_CONFIGURATION_PROPERTIES)
  private ConfigurationProperties configurationProperties;

  @Override
  public GlobalBindingContextProvider getObject() throws Exception {
    muleFunctionsBindingContextProvider.setConfigurationProperties(configurationProperties);
    return muleFunctionsBindingContextProvider;
  }

  @Override
  public Class<?> getObjectType() {
    return MuleFunctionsBindingContextProvider.class;
  }
}
