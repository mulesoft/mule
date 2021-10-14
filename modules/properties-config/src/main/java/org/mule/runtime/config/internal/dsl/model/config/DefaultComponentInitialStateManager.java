/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.config;

import static org.mule.runtime.api.source.SchedulerMessageSource.SCHEDULER_MESSAGE_SOURCE_IDENTIFIER;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.deployment.management.ComponentInitialStateManager;
import org.mule.runtime.api.util.MuleSystemProperties;

import javax.inject.Inject;

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
    ComponentLocation tempLocation = component.getLocation();
    TypedComponentIdentifier tempComponentIdentifier = tempLocation.getComponentIdentifier();
    ComponentIdentifier tempIdentifier = tempComponentIdentifier.getIdentifier();

    if (tempIdentifier.equals(SCHEDULER_MESSAGE_SOURCE_IDENTIFIER)) {
      return !configurationProperties.resolveBooleanProperty(MuleSystemProperties.DISABLE_SCHEDULER_SOURCES_PROPERTY)
          .orElse(false);
    } else {
      return true;
    }
  }

}
