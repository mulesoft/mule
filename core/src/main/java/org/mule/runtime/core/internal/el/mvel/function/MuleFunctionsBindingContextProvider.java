/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.mvel.function;

import static org.mule.runtime.api.metadata.DataType.fromFunction;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONFIGURATION_PROPERTIES;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionFunction;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.privileged.el.GlobalBindingContextProvider;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * {@link GlobalBindingContextProvider} that adds core function bindings.
 *
 * @since 4.0
 */
public class MuleFunctionsBindingContextProvider implements GlobalBindingContextProvider {

  @Inject
  @Named(OBJECT_CONFIGURATION_PROPERTIES)
  private ConfigurationProperties configurationProperties;

  @Inject
  @Named(ConfigurationComponentLocator.REGISTRY_KEY)
  private ConfigurationComponentLocator componentLocator;


  @Override
  public BindingContext getBindingContext() {
    BindingContext.Builder builder = BindingContext.builder();

    PropertyAccessFunction propertyFunction = new PropertyAccessFunction(configurationProperties);
    builder.addBinding("p", new TypedValue(propertyFunction, fromFunction(propertyFunction)));

    ExpressionFunction lookupFunction = new LookupFunction(componentLocator);
    builder.addBinding("lookup", new TypedValue(lookupFunction, fromFunction(lookupFunction)));

    return builder.build();
  }

}
