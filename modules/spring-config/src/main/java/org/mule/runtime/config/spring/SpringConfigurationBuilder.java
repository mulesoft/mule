/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.registry.Registry;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.api.i18n.I18nMessageFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Adds an existing Spring ApplicationContext to Mule's internal collection of Registries.
 */
public class SpringConfigurationBuilder extends AbstractConfigurationBuilder {

  private ApplicationContext appContext;

  private ApplicationContext parentContext;

  public SpringConfigurationBuilder(ApplicationContext appContext) {
    this.appContext = appContext;
  }

  public SpringConfigurationBuilder(ConfigurableApplicationContext appContext, ApplicationContext parentContext) {
    this.appContext = appContext;
    this.parentContext = parentContext;
  }

  protected void doConfigure(MuleContext muleContext) throws Exception {
    Registry registry;

    if (parentContext != null) {
      if (appContext instanceof ConfigurableApplicationContext) {
        registry = new SpringRegistry((ConfigurableApplicationContext) appContext, parentContext, muleContext);
      } else {
        throw new ConfigurationException(I18nMessageFactory
            .createStaticMessage("Cannot set a parent context if the ApplicationContext does not implement ConfigurableApplicationContext"));
      }
    } else {
      registry = new SpringRegistry(appContext, muleContext);
    }

    // Note: The SpringRegistry must be created before muleArtifactContext.refresh() gets called because
    // some beans may try to look up other beans via the Registry during preInstantiateSingletons().
    muleContext.addRegistry(registry);
    if (muleContext.getLifecycleManager().isPhaseComplete(Initialisable.PHASE_NAME)) {
      registry.initialise();
    }
  }

}
