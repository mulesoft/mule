/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.component.state;

import static org.mule.runtime.api.source.SchedulerMessageSource.SCHEDULER_MESSAGE_SOURCE_IDENTIFIER;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.deployment.management.ComponentInitialStateManager;
import org.mule.runtime.api.meta.AnnotatedObject;

import javax.inject.Inject;

/**
 * Default {@link ComponentInitialStateManager} that always signals initialization of components.
 */
public class DefaultComponentInitialStateManager implements ComponentInitialStateManager {

  @Inject
  private ConfigurationProperties configurationProperties;

  @Override
  public boolean mustStartMessageSource(AnnotatedObject component) {
    if (configurationProperties.resolveProperty(DISABLE_SCHEDULER_SOURCES_PROPERTY).isPresent()) {
      if (component.getLocation().getComponentIdentifier().getIdentifier().equals(SCHEDULER_MESSAGE_SOURCE_IDENTIFIER)) {
        return !configurationProperties.resolveBooleanProperty(DISABLE_SCHEDULER_SOURCES_PROPERTY).orElse(false);
      }
    }
    return true;
  }

}
