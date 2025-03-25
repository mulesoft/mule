/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.bean;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import jakarta.inject.Inject;

/**
 * A simple {@link Processor} that has the {@link MuleConfiguration} as an injected dependency.
 * <p>
 * Needs to also implement {@link Component} in order to avoid an error when creating a dynamic subclass to add the annotations,
 * because this package is not exported and the dynamic subclass is created with a different module (currently the unnamed).
 */
public class TestProcessorDependingOnMuleConfiguration extends AbstractComponent implements Processor {

  @javax.inject.Inject
  public MuleConfiguration muleConfigurationJavax;

  @Inject
  public MuleConfiguration muleConfiguration;

  @Override
  public CoreEvent process(CoreEvent event) {
    assertThat(muleConfiguration, sameInstance(muleConfigurationJavax));

    return CoreEvent.builder(event).message(Message.of(muleConfiguration.getDefaultErrorHandlerName())).build();
  }
}
