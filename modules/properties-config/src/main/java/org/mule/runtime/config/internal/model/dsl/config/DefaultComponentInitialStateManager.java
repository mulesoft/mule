/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model.dsl.config;

import static org.mule.runtime.api.source.SchedulerMessageSource.SCHEDULER_MESSAGE_SOURCE_IDENTIFIER;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.deployment.management.ComponentInitialStateManager;
import org.mule.runtime.api.util.MuleSystemProperties;

import jakarta.inject.Inject;

/**
 * Default {@link ComponentInitialStateManager} that always signals initialization of components.
 */
public class DefaultComponentInitialStateManager implements ComponentInitialStateManager {

  @Inject
  private ConfigurationProperties configurationProperties;

  @Override
  public boolean mustStartMessageSource(Component component) {
    if (!configurationProperties.resolveProperty(MuleSystemProperties.DISABLE_SCHEDULER_SOURCES_PROPERTY).isPresent()) {
      return true;
    }
    if (component.getIdentifier().equals(SCHEDULER_MESSAGE_SOURCE_IDENTIFIER)) {
      return !configurationProperties.resolveBooleanProperty(MuleSystemProperties.DISABLE_SCHEDULER_SOURCES_PROPERTY)
          .orElse(false);
    } else {
      return true;
    }
  }

}
