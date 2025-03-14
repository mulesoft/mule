/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.function;

import static org.mule.runtime.api.metadata.DataType.fromFunction;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONFIGURATION_PROPERTIES;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionFunction;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.privileged.el.GlobalBindingContextProvider;

import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * {@link GlobalBindingContextProvider} that adds core function bindings.
 *
 * @since 4.0
 */
public class MuleFunctionsBindingContextProvider implements GlobalBindingContextProvider {

  public static final String CORE_FUNCTIONS_PROVIDER_REGISTRY_KEY = "core.global.binding.provider";

  private ConfigurationProperties configurationProperties;

  @Inject
  @Named(ConfigurationComponentLocator.REGISTRY_KEY)
  private ConfigurationComponentLocator componentLocator;

  @Inject
  private ErrorTypeRepository errorTypeRepository;

  @Inject
  private SchedulerService schedulerService;

  private PropertyAccessFunction propertyFunction;

  @Override
  public BindingContext getBindingContext() {
    BindingContext.Builder builder = BindingContext.builder();

    propertyFunction = new PropertyAccessFunction();
    propertyFunction.setConfigurationProperties(configurationProperties);
    builder.addBinding("p", new TypedValue<>(propertyFunction, fromFunction(propertyFunction)));

    ExpressionFunction lookupFunction = new LookupFunction(componentLocator, schedulerService);
    builder.addBinding("lookup", new TypedValue<>(lookupFunction, fromFunction(lookupFunction)));

    ExpressionFunction causedByFunction = new CausedByFunction(errorTypeRepository);
    builder.addBinding("causedBy", new TypedValue<>(causedByFunction, fromFunction(causedByFunction)));

    return builder.build();
  }

  @Inject
  @Named(OBJECT_CONFIGURATION_PROPERTIES)
  public void setConfigurationProperties(ConfigurationProperties configurationProperties) {
    this.configurationProperties = configurationProperties;
    if (propertyFunction != null) {
      propertyFunction.setConfigurationProperties(configurationProperties);
    }
  }
}
